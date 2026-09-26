package com.ruta.deliverypin.domain.port.in;

import com.ruta.deliverypin.domain.model.StoredPhoto;

import java.util.Optional;

public interface GetDriverDeliveryPhotoUseCase {

    Optional<StoredPhoto> getPhoto(Long attemptId, Long driverId);
}
