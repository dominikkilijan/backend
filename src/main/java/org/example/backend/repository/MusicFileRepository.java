package org.example.backend.repository;

import org.example.backend.model.MusicFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MusicFileRepository extends JpaRepository<MusicFile, UUID> {

}
