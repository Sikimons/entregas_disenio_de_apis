import { useRef, type ChangeEvent, type ClipboardEvent, type KeyboardEvent } from 'react'

export const PIN_LENGTH = 6

interface PinBoxesProps {
  value: string
  onChange: (value: string) => void
  disabled?: boolean
}

export function PinBoxes({ value, onChange, disabled }: PinBoxesProps) {
  const inputRefs = useRef<(HTMLInputElement | null)[]>([])
  const digits = value.split('')
  while (digits.length < PIN_LENGTH) digits.push('')

  const setDigit = (index: number, char: string) => {
    const next = [...digits]
    next[index] = char
    onChange(next.join('').slice(0, PIN_LENGTH))
  }

  const handleChange = (index: number, e: ChangeEvent<HTMLInputElement>) => {
    const raw = e.target.value.replace(/\D/g, '')
    if (!raw) {
      setDigit(index, '')
      return
    }
    const chars = raw.split('')
    const next = [...digits]
    chars.forEach((c, i) => { if (index + i < PIN_LENGTH) next[index + i] = c })
    onChange(next.join('').slice(0, PIN_LENGTH))
    const nextIndex = Math.min(index + chars.length, PIN_LENGTH - 1)
    inputRefs.current[nextIndex]?.focus()
  }

  const handleKeyDown = (index: number, e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus()
    }
  }

  return (
    <div className="pin-boxes">
      {digits.map((digit, index) => (
        <input
          key={index}
          ref={(el) => { inputRefs.current[index] = el }}
          value={digit}
          inputMode="numeric"
          aria-label={`Dígito ${index + 1} del PIN`}
          maxLength={1}
          className={digit ? 'filled' : ''}
          disabled={disabled}
          onChange={(e) => handleChange(index, e)}
          onPaste={(e: ClipboardEvent<HTMLInputElement>) => {
            const raw = e.clipboardData.getData('text').replace(/\D/g, '').slice(0, PIN_LENGTH)
            if (raw) { e.preventDefault(); onChange(raw); inputRefs.current[Math.min(raw.length, PIN_LENGTH - 1)]?.focus() }
          }}
          onKeyDown={(e) => handleKeyDown(index, e)}
          autoFocus={index === 0}
        />
      ))}
    </div>
  )
}
