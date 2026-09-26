package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.model.PageRequest;
import com.ruta.deliverypin.domain.model.StoredPhoto;
import com.ruta.deliverypin.domain.port.in.GetDeliveryPhotoUseCase;
import com.ruta.deliverypin.domain.port.in.ListDeliveryHistoryUseCase;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.DeliveryAttemptResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.PageResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/deliveries")
public class AdminDeliveryHistoryController {

    private final ListDeliveryHistoryUseCase listDeliveryHistoryUseCase;
    private final GetDeliveryPhotoUseCase getDeliveryPhotoUseCase;

    public AdminDeliveryHistoryController(
            ListDeliveryHistoryUseCase listDeliveryHistoryUseCase,
            GetDeliveryPhotoUseCase getDeliveryPhotoUseCase
    ) {
        this.listDeliveryHistoryUseCase = listDeliveryHistoryUseCase;
        this.getDeliveryPhotoUseCase = getDeliveryPhotoUseCase;
    }

    @GetMapping
    public PageResponse<DeliveryAttemptResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = listDeliveryHistoryUseCase.list(new PageRequest(page, size));
        return PageResponse.from(result, DeliveryAttemptResponse::from);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        return getDeliveryPhotoUseCase.getPhoto(id)
                .map(this::toImageResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> toImageResponse(StoredPhoto photo) {
        MediaType mediaType = photo.contentType() != null
                ? MediaType.parseMediaType(photo.contentType())
                : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok().contentType(mediaType).body(photo.data());
    }
}
