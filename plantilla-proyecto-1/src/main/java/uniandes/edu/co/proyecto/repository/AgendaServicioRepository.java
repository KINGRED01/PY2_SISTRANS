package uniandes.edu.co.proyecto.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import uniandes.edu.co.proyecto.interfaz.DisponibilidadServicioProjection;
import uniandes.edu.co.proyecto.interfaz.IndiceUsoServicioProjection;
import uniandes.edu.co.proyecto.modelo.AgendaServicio;

public interface AgendaServicioRepository extends JpaRepository<AgendaServicio, Integer> {

    // obtener todos los agenda servicio
    @Query(value="SELECT * FROM agendaservicio", nativeQuery=true)
    Collection<AgendaServicio> darAgendaServicios();

    // obtener agenda servicio por id
    @Query(value = "SELECT * FROM agendaservicio WHERE id = :id", nativeQuery = true)
    AgendaServicio darAgendaServicio(@Param("id") Integer id);

    // insertar agenda servicio RF7.1 vacía
    @Modifying
    @Transactional
    @Query(value="INSERT INTO agendaservicio (id_medico, id_serviciosalud, id_ips, fecha_Hora, id_afiliado, id_ordenservicio) VALUES (:id_medico, :id_serviciosalud, :id_ips, :fecha_Hora, NULL, NULL)", nativeQuery=true)
    void insertarAgendaServicio(@Param("id_medico") Integer id_medico, @Param("id_serviciosalud") Integer id_serviciosalud, @Param("id_ips") Integer id_ips, @Param("fecha_Hora") LocalDateTime fecha_Hora);

    // insertar agenda servicio RF7.2 agendamiento
    @Modifying
    @Transactional
    @Query(value="UPDATE agendaservicio SET id_afiliado = :id_afiliado, id_ordenservicio = :id_ordenservicio WHERE id = :id AND id_afiliado IS NULL", nativeQuery=true)
    int AgendarServicio(@Param("id") Integer id, @Param("id_afiliado") Integer id_afiliado, @Param("id_ordenservicio") Integer id_ordenservicio);

    // actualizar una agenda servicio
    @Modifying
    @Transactional
    @Query(value="UPDATE agendaservicio SET id_medico = :id_medico, id_serviciosalud = :id_serviciosalud, id_ips = :id_ips, fecha_Hora = :fecha_Hora, id_afiliado = :id_afiliado, id_ordenservicio = :id_ordenservicio WHERE id = :id", nativeQuery=true)
    void actualizarAgendaServicio(@Param("id") Integer id, @Param("id_medico") Integer id_medico, @Param("id_serviciosalud") Integer id_serviciosalud, @Param("id_ips") Integer id_ips, @Param("fecha_Hora") LocalDateTime fechaHora, @Param("id_afiliado") Integer id_afiliado, @Param("id_ordenservicio") Integer id_ordenservicio);

    // eliminar agenda servicio
    @Modifying
    @Transactional
    @Query(value="DELETE FROM agendaservicio WHERE id = :id", nativeQuery=true)
    void eliminarAgendaServicio(@Param("id") Integer id);

    // RFC1 - Consultar disponibilidad de un servicio de salud
    @Query(value = """
    SELECT 
        s.nombre as nombreServicio,
        a.fecha_Hora as fechaHora,
        i.nombre as nombreIPS,
        m.nombre as nombreMedico
    FROM 
        agendaservicio a
    JOIN 
        serviciosalud s ON a.id_serviciosalud = s.id
    JOIN 
        ips i ON a.id_ips = i.id
    JOIN 
        medico m ON a.id_medico = m.id
    WHERE 
        a.id_serviciosalud = :idServicio
        AND a.id_afiliado IS NULL
        AND a.id_ordenservicio IS NULL
        AND a.fecha_Hora BETWEEN :fechaInicio AND :fechaFin
    ORDER BY 
        a.fecha_Hora ASC
    """, nativeQuery = true)
    List<DisponibilidadServicioProjection> findDisponibilidadByServicio(
        @Param("idServicio") Integer idServicio,
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin);

    // RFC3 - Índice de uso de servicios
    @Query(value = """
    SELECT 
        ss.id as idServicio,
        ss.nombre as nombreServicio,
        COUNT(DISTINCT CASE WHEN a.id_afiliado IS NULL THEN a.id END) as totalDisponibles,
        COUNT(DISTINCT p.id) as totalUsados,
        CASE 
            WHEN COUNT(DISTINCT p.id) = 0 THEN 0
            ELSE ROUND(COUNT(DISTINCT p.id) / (COUNT(DISTINCT CASE WHEN a.id_afiliado IS NULL THEN a.id END) + COUNT(DISTINCT p.id)), 2)
        END as indiceUso
    FROM 
        serviciosalud ss
    LEFT JOIN 
        agendaservicio a ON ss.id = a.id_serviciosalud
        AND a.fecha_Hora BETWEEN :fechaInicio AND :fechaFin
    LEFT JOIN 
        prestacion p ON ss.id = p.id_serviciosalud
        AND p.fecha_Hora_Final BETWEEN :fechaInicio AND :fechaFin
    GROUP BY 
        ss.id, ss.nombre
    ORDER BY 
        indiceUso DESC
    """, nativeQuery = true)
    List<IndiceUsoServicioProjection> calcularIndiceUsoServicios(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin);

    // RFC5 - NUEVO método para consultar disponibilidad filtrada (SERIALIZABLE)
    @Query("""
        SELECT a FROM AgendaServicio a
        WHERE (:fechaInicio IS NULL OR a.fechaHora >= :fechaInicio)
        AND (:fechaFin IS NULL OR a.fechaHora <= :fechaFin)
        AND (:idMedico IS NULL OR a.id_medico.id = :idMedico)
        AND (:idServicio IS NULL OR a.id_serviciosalud.id = :idServicio)
    """)
    List<AgendaServicio> findDisponibilidadPorFiltros(
        @Param("fechaInicio") LocalDateTime fechaInicio,
        @Param("fechaFin") LocalDateTime fechaFin,
        @Param("idMedico") Long idMedico,
        @Param("idServicio") Long idServicio
    );

}



