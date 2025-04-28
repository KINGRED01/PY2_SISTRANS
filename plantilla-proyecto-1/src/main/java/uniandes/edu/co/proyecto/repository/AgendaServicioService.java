package uniandes.edu.co.proyecto.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

import uniandes.edu.co.proyecto.modelo.AgendaServicio;
import uniandes.edu.co.proyecto.modelo.OrdenServicio;
import uniandes.edu.co.proyecto.repository.AgendaServicioRepository;
import uniandes.edu.co.proyecto.repository.OrdenServicioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AgendaServicioService {

    @Autowired
    private AgendaServicioRepository agendaServicioRepository;

    @Autowired
    private OrdenServicioRepository ordenServicioRepository;

    /**
     * Transacción que permite agendar un servicio de salud para un afiliado.
     * Confirma disponibilidad y registra la orden correspondiente.
     * Hace commit solo si todas las operaciones tienen éxito; rollback en caso contrario.
     *
     * @param idAgendaServicio ID del espacio de agenda seleccionado.
     * @param nuevaOrden Detalles de la orden de servicio a crear.
     * @return true si se agenda exitosamente, false si no hay disponibilidad.
     */
    @Transactional
    public boolean agendarServicio(Integer idAgendaServicio, OrdenServicio nuevaOrden) {
        Optional<AgendaServicio> agendaOptional = agendaServicioRepository.findById(idAgendaServicio);

        if (agendaOptional.isPresent()) {
            AgendaServicio agenda = agendaOptional.get();

            // Verificar disponibilidad antes de agendar
            if (agenda.getId_afiliado() == null && agenda.getId_ordenservicio() == null) {
                // Agendar servicio: asociar afiliado y orden
                agenda.setId_afiliado(nuevaOrden.getId_afiliado());
                agenda.setId_ordenservicio(nuevaOrden);
                agendaServicioRepository.save(agenda);

                // Guardar la orden
                ordenServicioRepository.save(nuevaOrden);

                return true;
            }
        }
        return false;
    }

    /**
     * Transacción SERIALIZABLE que consulta disponibilidad de servicios con filtros dinámicos.
     * Pausa 30 segundos antes de la consulta para pruebas de concurrencia.
     *
     * @param fechaInicio Fecha inicial de búsqueda (opcional).
     * @param fechaFin Fecha final de búsqueda (opcional).
     * @param idMedico ID del médico (opcional).
     * @param idServicio ID del servicio de salud (opcional).
     * @return Lista de AgendaServicio disponible que cumple los filtros.
     * @throws InterruptedException Si ocurre error en el sleep.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<AgendaServicio> consultarDisponibilidadConFiltros(
            LocalDateTime fechaInicio,
            LocalDateTime fechaFin,
            Long idMedico,
            Long idServicio) throws InterruptedException {
        try {
            // Esperar 30 segundos antes de consultar para pruebas de concurrencia
            Thread.sleep(30000);

            return agendaServicioRepository.findDisponibilidadPorFiltros(fechaInicio, fechaFin, idMedico, idServicio);
        } catch (Exception e) {
            throw new RuntimeException("Error en la consulta de disponibilidad", e);
        }
    }
}
