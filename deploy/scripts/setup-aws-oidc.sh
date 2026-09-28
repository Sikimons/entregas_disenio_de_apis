#!/usr/bin/env bash
# Crea (o actualiza, si ya existe) la infraestructura de AWS que necesita
# deploy-backend en cd.yml: el proveedor OIDC de GitHub Actions y el rol IAM que
# asume, con permisos minimos de SSM Run Command. NO crea la(s) EC2 -- eso es
# infraestructura propia de cada quien (instance type, VPC, AMI); este script solo
# prepara el lado de AWS que el pipeline necesita para poder desplegar en ellas.
#
# Requiere: aws cli configurado con credenciales que puedan crear roles/politicas IAM
# y (la primera vez) el proveedor OIDC. Correrlo desde tu maquina, no desde CI.
#
# Uso:
#   AWS_REGION=us-east-1 ./deploy/scripts/setup-aws-oidc.sh
#
# Variables (todas opcionales salvo AWS_REGION):
#   GITHUB_ORG          Default: ups-master
#   GITHUB_REPO         Default: ruta-delivery
#   ROLE_NAME           Default: gha-ruta-deploy
#   SSM_INSTANCE_IDS    IDs de EC2 separados por espacio (p. ej. "i-0abc i-0def"),
#                        para acotar ssm:SendCommand a esas instancias puntuales.
#                        Sin esto, el permiso queda en "*" (todas las instancias de
#                        la cuenta/region) -- se avisa igual, corregilo apenas tengas
#                        el/los instance ID reales.
#   BACKUP_S3_BUCKET    Si usas BACKUP_S3_URI en deploy/env/<entorno>.env, el nombre
#                        del bucket (sin "s3://"), para agregar s3:PutObject.
#
# Al final imprime el Role ARN y los comandos "gh variable set" listos para copiar,
# uno por Environment (staging/production) -- con EC2_INSTANCE_ID como placeholder,
# porque la instancia se crea aparte.
set -euo pipefail

: "${AWS_REGION:?Definir AWS_REGION, p. ej. AWS_REGION=us-east-1}"
GITHUB_ORG="${GITHUB_ORG:-ups-master}"
GITHUB_REPO="${GITHUB_REPO:-ruta-delivery}"
ROLE_NAME="${ROLE_NAME:-gha-ruta-deploy}"
SSM_INSTANCE_IDS="${SSM_INSTANCE_IDS:-}"
BACKUP_S3_BUCKET="${BACKUP_S3_BUCKET:-}"

command -v aws >/dev/null || { echo "Falta aws cli. Instalalo primero: https://aws.amazon.com/cli/" >&2; exit 1; }
ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
echo "Cuenta AWS: $ACCOUNT_ID | Region: $AWS_REGION | Repo: $GITHUB_ORG/$GITHUB_REPO"

# --- 1. Proveedor OIDC de GitHub Actions (una sola vez por cuenta, se comparte entre
#         todos los repos/roles que lo usen) ---
OIDC_URL="token.actions.githubusercontent.com"
OIDC_ARN="arn:aws:iam::$ACCOUNT_ID:oidc-provider/$OIDC_URL"

if aws iam get-open-id-connect-provider --open-id-connect-provider-arn "$OIDC_ARN" >/dev/null 2>&1; then
  echo "Proveedor OIDC ya existe: $OIDC_ARN"
else
  echo "Creando proveedor OIDC..."
  # AWS solo usa este thumbprint como respaldo si no logra validar la cadena de
  # certificados via sus CA de confianza (caso normal para token.actions.githubusercontent.com,
  # que usa una CA publica) -- se calcula en vivo en vez de hardcodear un valor que
  # puede quedar desactualizado si GitHub rota el certificado.
  THUMBPRINT="$(echo | openssl s_client -servername "$OIDC_URL" -showcerts -connect "$OIDC_URL:443" 2>/dev/null \
    | openssl x509 -fingerprint -sha1 -noout | sed 's/.*=//' | tr -d ':' | tr 'A-Z' 'a-z')"
  aws iam create-open-id-connect-provider \
    --url "https://$OIDC_URL" \
    --client-id-list "sts.amazonaws.com" \
    --thumbprint-list "$THUMBPRINT" >/dev/null
  echo "Proveedor OIDC creado: $OIDC_ARN"
fi

# --- 2. Rol IAM, con trust policy limitada a los Environments de este repo ---
TRUST_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [{
    "Effect": "Allow",
    "Principal": { "Federated": "$OIDC_ARN" },
    "Action": "sts:AssumeRoleWithWebIdentity",
    "Condition": {
      "StringEquals": { "$OIDC_URL:aud": "sts.amazonaws.com" },
      "StringLike": { "$OIDC_URL:sub": [
        "repo:$GITHUB_ORG/$GITHUB_REPO:environment:staging",
        "repo:$GITHUB_ORG/$GITHUB_REPO:environment:production"
      ] }
    }
  }]
}
EOF
)

if aws iam get-role --role-name "$ROLE_NAME" >/dev/null 2>&1; then
  echo "Rol $ROLE_NAME ya existe, actualizando su trust policy..."
  aws iam update-assume-role-policy --role-name "$ROLE_NAME" --policy-document "$TRUST_POLICY"
else
  echo "Creando rol $ROLE_NAME..."
  aws iam create-role --role-name "$ROLE_NAME" --assume-role-policy-document "$TRUST_POLICY" >/dev/null
fi
ROLE_ARN="$(aws iam get-role --role-name "$ROLE_NAME" --query Role.Arn --output text)"

# --- 3. Politica inline: solo lo que deploy-backend necesita (SSM Run Command; S3
#         opcional para respaldos) ---
if [ -n "$SSM_INSTANCE_IDS" ]; then
  INSTANCE_ARNS_JSON="["
  for id in $SSM_INSTANCE_IDS; do
    INSTANCE_ARNS_JSON+="\"arn:aws:ec2:$AWS_REGION:$ACCOUNT_ID:instance/$id\","
  done
  INSTANCE_ARNS_JSON="${INSTANCE_ARNS_JSON%,}]"
else
  echo "AVISO: SSM_INSTANCE_IDS vacio -- ssm:SendCommand queda con Resource \"*\"" \
    "(cualquier instancia de esta cuenta/region). Volve a correr este script con" \
    "SSM_INSTANCE_IDS=\"i-xxxx ...\" en cuanto tengas la(s) EC2 creada(s)." >&2
  INSTANCE_ARNS_JSON='["*"]'
fi

S3_STATEMENT=""
if [ -n "$BACKUP_S3_BUCKET" ]; then
  S3_STATEMENT=",{
    \"Effect\": \"Allow\",
    \"Action\": \"s3:PutObject\",
    \"Resource\": \"arn:aws:s3:::$BACKUP_S3_BUCKET/*\"
  }"
fi

PERMISSIONS_POLICY=$(cat <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "ssm:SendCommand",
      "Resource": $INSTANCE_ARNS_JSON
    },
    {
      "Effect": "Allow",
      "Action": "ssm:SendCommand",
      "Resource": "arn:aws:ssm:$AWS_REGION::document/AWS-RunShellScript"
    },
    {
      "Effect": "Allow",
      "Action": "ssm:GetCommandInvocation",
      "Resource": "*"
    }$S3_STATEMENT
  ]
}
EOF
)
aws iam put-role-policy --role-name "$ROLE_NAME" --policy-name "ruta-deploy-ssm" --policy-document "$PERMISSIONS_POLICY"

echo
echo "== Listo =="
echo "Role ARN: $ROLE_ARN"
echo
echo "Falta, por tu cuenta: crear la(s) EC2 con un instance profile que incluya la"
echo "policy administrada AmazonSSMManagedInstanceCore (ver README -> Setup de CD)."
echo
echo "Cuando tengas AWS_REGION, el Role ARN de arriba y el/los EC2_INSTANCE_ID, carga"
echo "las variables de cada Environment (no son secretos, son 'variables' de GitHub):"
echo
for env in staging production; do
  cat <<CMD
gh variable set AWS_REGION --repo $GITHUB_ORG/$GITHUB_REPO --env $env --body "$AWS_REGION"
gh variable set AWS_DEPLOY_ROLE_ARN --repo $GITHUB_ORG/$GITHUB_REPO --env $env --body "$ROLE_ARN"
gh variable set EC2_INSTANCE_ID --repo $GITHUB_ORG/$GITHUB_REPO --env $env --body "i-xxxxxxxxxxxxxxxxx"  # reemplazar
CMD
  echo
done
