package me.crespel.karaplan.service;

import java.util.Optional;
import java.util.Set;

import me.crespel.karaplan.domain.Song;

public interface SongService {

	Optional<Song> findById(Long id);

	Optional<Song> findByCatalogId(Long catalogId);

	Set<Song> findAll();

	Set<Song> search(String query, Integer limit, Integer offset);

	Song save(Song song);

}
