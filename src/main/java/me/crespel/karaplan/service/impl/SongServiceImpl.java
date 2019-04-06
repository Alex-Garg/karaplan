package me.crespel.karaplan.service.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import me.crespel.karaplan.domain.Song;
import me.crespel.karaplan.domain.SongComment;
import me.crespel.karaplan.domain.SongVote;
import me.crespel.karaplan.domain.User;
import me.crespel.karaplan.model.CatalogSong;
import me.crespel.karaplan.model.CatalogSongList;
import me.crespel.karaplan.repository.SongCommentRepo;
import me.crespel.karaplan.repository.SongRepo;
import me.crespel.karaplan.repository.SongVoteRepo;
import me.crespel.karaplan.service.ArtistService;
import me.crespel.karaplan.service.CatalogService;
import me.crespel.karaplan.service.SongService;

@Service
public class SongServiceImpl implements SongService {

	@Autowired
	protected SongRepo songRepo;

	@Autowired
	protected SongCommentRepo songCommentRepo;

	@Autowired
	protected SongVoteRepo songVoteRepo;

	@Autowired
	protected CatalogService catalogService;

	@Autowired
	protected ArtistService artistService;

	protected final ConfigurableConversionService conversionService;

	public SongServiceImpl() {
		conversionService = new DefaultConversionService();
		conversionService.addConverter(new CatalogSongToSongConverter());
	}

	@Override
	public Set<Song> findAll() {
		return Sets.newLinkedHashSet(songRepo.findAll());
	}

	@Override
	public Set<Song> findAll(Pageable pageable) {
		return Sets.newLinkedHashSet(songRepo.findAll(pageable));
	}

	@Override
	public Optional<Song> findById(Long id) {
		return songRepo.findById(id);
	}

	@Override
	public Optional<Song> findByCatalogId(Long catalogId) {
		Optional<Song> song = songRepo.findByCatalogId(catalogId);
		if (!song.isPresent()) {
			song = Optional.ofNullable(conversionService.convert(catalogService.getSong(catalogId), Song.class));
		}
		return song;
	}

	@Override
	public Set<Song> search(String query, Pageable pageable) {
		Set<Song> resultSongs = Sets.newLinkedHashSet();

		CatalogSongList catalogSongList = catalogService.getSongList(query, pageable.getPageSize(), pageable.getOffset());
		if (catalogSongList.getSongs() != null) {
			// Convert catalog songs
			Set<Song> catalogSongs = catalogSongList.getSongs().stream()
					.map(it -> conversionService.convert(it, Song.class))
					.collect(Collectors.toCollection(LinkedHashSet::new));

			// Find all catalog IDs
			Set<Long> catalogSongIds = catalogSongs.stream()
					.map(Song::getCatalogId)
					.collect(Collectors.toCollection(LinkedHashSet::new));

			// Find all matching local songs
			Set<Song> localSongs = Sets.newLinkedHashSet(songRepo.findAllByCatalogIdIn(catalogSongIds));

			// Merge results
			resultSongs.addAll(Stream.of(catalogSongs, localSongs)
					.flatMap(Collection::stream)
					.collect(Collectors.toMap(Song::getCatalogId, Function.identity(), (catalog, local) -> local, LinkedHashMap::new))
					.values());
		}
		return resultSongs;
	}

	@Override
	public Song save(Song song) {
		return songRepo.save(song);
	}

	@Override
	public SongComment addComment(Song song, User user, String comment) {
		SongComment songComment = new SongComment()
				.setSong(song)
				.setUser(user)
				.setComment(comment);
		return songCommentRepo.save(songComment);
	}

	@Override
	public SongVote vote(Song song, User user, int score) {
		SongVote songVote = songVoteRepo.findBySongAndUser(song, user).orElseGet(() -> new SongVote().setSong(song).setUser(user));
		if (score == 0) {
			if (songVote.getId() != null) {
				songVoteRepo.delete(songVote);
			}
			return songVote;
		} else {
			songVote.setScore(score > 0 ? 1 : -1);
			return songVoteRepo.save(songVote);
		}
	}

	public class CatalogSongToSongConverter implements Converter<CatalogSong, Song> {

		@Override
		public Song convert(CatalogSong source) {
			return new Song()
					.setCatalogId(source.getId())
					.setName(source.getName())
					.setDuration(source.getDuration())
					.setImage(source.getImg())
					.setLyrics(source.getLyrics())
					.setArtist(artistService.findByCatalogId(source.getArtist().getId()).orElse(null));
		}

	}

}
