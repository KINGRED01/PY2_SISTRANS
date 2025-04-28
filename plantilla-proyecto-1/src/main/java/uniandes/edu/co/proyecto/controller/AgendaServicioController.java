package uniandes.edu.co.proyecto.controller;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uniandes.edu.co.proyecto.interfaz.DisponibilidadServicioProjection;
import uniandes.edu.co.proyecto.interfaz.IndiceUsoServicioProjection;
import uniandes.edu.co.proyecto.modelo.AgendaServicio;
import uniandes.edu.co.proyecto.modelo.OrdenServicio;
import uniandes.edu.co.proyecto.repository.AgendaServicioRepository;
import uniandes.edu.co.proyecto.repository.AgendaServicioService;

@RestController
public class AgendaServicioController {

    @Autowired
    private AgendaServicioRepository agendaServicioRepository;

    @Autowired
    private AgendaServicioService agendaServicioService; // <- IMPORTANTE: Agregado para RF9 y RFC5

    // obtener los agenda servicio
    @GetMapping("/agenda-servicio")
    public ResponseEntity<Collection<AgendaServicio>> agendas() {
        try {
            Collection<AgendaServicio> agendaservicios = agendaServicioRepository.darAgendaServicios();
            return ResponseEntity.ok(agendaservicios);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // insertar agenda servicio (sin afiliado sin orden)
    @PostMapping("/agenda-servicio/new/save")
    public ResponseEntity<?> crearAgendaServicio(@RequestBody AgendaServicio agenda) {
        try {
            
            if (agenda.getId_medico() == null) {
                return ResponseEntity.badRequest().body("El médico es obligatorio");
            }
            
            if (agenda.getId_serviciosalud() == null) {
                return ResponseEntity.badRequest().body("El servicio de salud es obligatorio");
            }
            
            if (agenda.getId_ips() == null) {
                return ResponseEntity.badRequest().body("La IPS es obligatoria");
            }
            
            if (agenda.getFechaHora() == null) {
                return ResponseEntity.badRequest().body("La fecha y hora son obligatorias");
            }
            agendaServicioRepository.insertarAgendaServicio(
                agenda.getId_medico().getId(),
                agenda.getId_serviciosalud().getId(),
                agenda.getId_ips().getId(),
                agenda.getFechaHora()
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body("Agenda de servicio creada exitosamente");
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                   .body("Error al crear la agenda de servicio: " + e.getMessage());
        }
    }




    //insertar agenda servicio (afiliado reserva su srvicio)
    @PutMapping("/agenda-servicio/{id}/reservar")  
    public ResponseEntity<?> reservarAgenda(
            @PathVariable Integer id,
            @RequestBody Map<String, Integer> request) {
        
        try {
            
            if (request.get("idAfiliado") == null || request.get("idOrdenServicio") == null) {
                return ResponseEntity.badRequest().body("Se requieren idAfiliado e idOrdenServicio");
            }

           
            int updatedRows = agendaServicioRepository.AgendarServicio(
                id,
                request.get("idAfiliado"),
                request.get("idOrdenServicio")
            );

            if (updatedRows == 0) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                       .body("No se pudo reservar. La agenda puede no existir o ya estar reservada");
            }

            return ResponseEntity.ok("Agenda reservada exitosamente");

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                   .body("Error al reservar agenda: " + e.getMessage());
        }
    }

    // RFC1 - Consultar disponibilidad de un servicio
    @GetMapping("/agenda-servicio/disponibilidad/{idServicio}")
    public ResponseEntity<?> consultarDisponibilidadServicio(
        @PathVariable Integer idServicio) {
        try {
            LocalDateTime fechaActual = LocalDateTime.now();
            LocalDateTime fechaFin = fechaActual.plusWeeks(4);

            List<DisponibilidadServicioProjection> disponibilidad =
                agendaServicioRepository.findDisponibilidadByServicio(
                    idServicio,
                    fechaActual,
                    fechaFin
                );

            if (disponibilidad.isEmpty()) {
                return ResponseEntity.ok("No hay disponibilidad para este servicio en las próximas 4 semanas");
            }
            return ResponseEntity.ok(disponibilidad);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                   .body("Error al consultar disponibilidad: " + e.getMessage());
        }
    }

    // RFC3 - Índice de uso de servicios
    @GetMapping("/agenda-servicio/indice-uso")
    public ResponseEntity<?> getIndiceUsoServicios(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        try {
            if (fechaFin.isBefore(fechaInicio)) {
                return ResponseEntity.badRequest().body("La fecha final debe ser posterior a la fecha inicial");
            }
            List<IndiceUsoServicioProjection> indices =
                agendaServicioRepository.calcularIndiceUsoServicios(fechaInicio, fechaFin);

            if (indices.isEmpty()) {
                return ResponseEntity.ok("No hay datos disponibles para el período especificado");
            }
            return ResponseEntity.ok(indices);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                   .body("Error al calcular índices de uso: " + e.getMessage());
        }
    }

    // RF9 - Agendar servicio de salud (TRANSACCIONAL)
    @PostMapping("/agenda-servicio/agendar-transaccional/{idAgendaServicio}")
    public ResponseEntity<String> agendarServicioTransaccional(
            @PathVariable("idAgendaServicio") Long idAgendaServicio,
            @RequestBody OrdenServicio nuevaOrden) {
        try {
            boolean resultado = agendaServicioService.agendarServicio(idAgendaServicio, nuevaOrden);
            if (resultado) {
                return ResponseEntity.ok("Agendamiento exitoso.");
            } else {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                       .body("No se pudo agendar el servicio (posiblemente ya no hay disponibilidad).");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                   .body("Error al agendar servicio: " + e.getMessage());
        }
    }

    // RFC5 - Consultar disponibilidad de agenda (SERIALIZABLE + 30s espera)
    @GetMapping("/agenda-servicio/disponibilidad-transaccional")
    public ResponseEntity<?> consultarDisponibilidadTransaccional(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin,
            @RequestParam(required = false) Long idMedico,
            @RequestParam(required = false) Long idServicio) {
        try {
            List<AgendaServicio> resultados = agendaServicioService.consultarDisponibilidadConFiltros(
                fechaInicio, fechaFin, idMedico, idServicio
            );
            return ResponseEntity.ok(resultados);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                   .body("Error al consultar disponibilidad: " + e.getMessage());
        }
    }

}
