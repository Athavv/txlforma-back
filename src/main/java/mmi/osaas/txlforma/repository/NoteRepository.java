package mmi.osaas.txlforma.repository;

import mmi.osaas.txlforma.model.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {
    
    @Query("SELECT note FROM Note note " +
           "JOIN FETCH note.participation participation " +
           "JOIN FETCH participation.user " +
           "JOIN FETCH note.givenBy " +
           "WHERE participation.id = :participationId")
    Optional<Note> findByParticipationId(@Param("participationId") Long participationId);
    
    @Query("SELECT note FROM Note note " +
           "JOIN FETCH note.participation participation " +
           "JOIN FETCH participation.user " +
           "JOIN FETCH note.givenBy " +
           "WHERE participation.session.id = :sessionId")
    List<Note> findByParticipationSessionId(@Param("sessionId") Long sessionId);
    
    @Query("SELECT note FROM Note note " +
           "JOIN FETCH note.participation participation " +
           "JOIN FETCH participation.user " +
           "JOIN FETCH note.givenBy " +
           "WHERE participation.user.id = :userId")
    List<Note> findByParticipationUserId(@Param("userId") Long userId);
}

