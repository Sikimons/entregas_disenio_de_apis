package com.ruta.deliverypin.infrastructure.adapter.in.web;

import com.ruta.deliverypin.domain.model.Driver;
import com.ruta.deliverypin.domain.port.in.CreateDriverUseCase;
import com.ruta.deliverypin.domain.port.in.DeleteDriverUseCase;
import com.ruta.deliverypin.domain.port.in.ListDriversUseCase;
import com.ruta.deliverypin.domain.port.in.UpdateDriverUseCase;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.CreateDriverRequest;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.DriverResponse;
import com.ruta.deliverypin.infrastructure.adapter.in.web.dto.UpdateDriverRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminDriverController {

    private final CreateDriverUseCase createDriverUseCase;
    private final UpdateDriverUseCase updateDriverUseCase;
    private final DeleteDriverUseCase deleteDriverUseCase;
    private final ListDriversUseCase listDriversUseCase;

    public AdminDriverController(
            CreateDriverUseCase createDriverUseCase,
            UpdateDriverUseCase updateDriverUseCase,
            DeleteDriverUseCase deleteDriverUseCase,
            ListDriversUseCase listDriversUseCase
    ) {
        this.createDriverUseCase = createDriverUseCase;
        this.updateDriverUseCase = updateDriverUseCase;
        this.deleteDriverUseCase = deleteDriverUseCase;
        this.listDriversUseCase = listDriversUseCase;
    }

    @GetMapping
    public List<DriverResponse> list() {
        return listDriversUseCase.listAll().stream().map(DriverResponse::from).toList();
    }

    @PostMapping
    public DriverResponse create(@Valid @RequestBody CreateDriverRequest request) {
        Driver created = createDriverUseCase.create(new CreateDriverUseCase.CreateDriverCommand(
                request.username(), request.password(), request.fullName(), request.role()
        ));
        return DriverResponse.from(created);
    }

    @PutMapping("/{id}")
    public DriverResponse update(@PathVariable Long id, @Valid @RequestBody UpdateDriverRequest request) {
        Driver updated = updateDriverUseCase.update(id, new UpdateDriverUseCase.UpdateDriverCommand(
                request.fullName(), request.active(), request.password()
        ));
        return DriverResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        deleteDriverUseCase.delete(id);
    }
}
