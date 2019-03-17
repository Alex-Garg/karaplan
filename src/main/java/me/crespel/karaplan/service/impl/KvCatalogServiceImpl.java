package me.crespel.karaplan.service.impl;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.crespel.karaplan.config.KvConfig.KvProperties;
import me.crespel.karaplan.model.CatalogArtist;
import me.crespel.karaplan.model.CatalogSelectionList;
import me.crespel.karaplan.model.CatalogSelectionType;
import me.crespel.karaplan.model.CatalogSong;
import me.crespel.karaplan.model.CatalogSongList;
import me.crespel.karaplan.model.CatalogSongListType;
import me.crespel.karaplan.model.exception.TechnicalException;
import me.crespel.karaplan.model.kv.KvArtist;
import me.crespel.karaplan.model.kv.KvArtistResponse;
import me.crespel.karaplan.model.kv.KvQuery;
import me.crespel.karaplan.model.kv.KvSong;
import me.crespel.karaplan.model.kv.KvSongList;
import me.crespel.karaplan.model.kv.KvSongResponse;
import me.crespel.karaplan.service.CatalogService;

@Service("kvCatalog")
public class KvCatalogServiceImpl implements CatalogService {

	@Autowired
	private KvProperties properties;

	@Autowired
	private RestTemplate restTemplate;

	protected final ConfigurableConversionService conversionService;

	protected final ObjectMapper jsonMapper = new ObjectMapper();

	public KvCatalogServiceImpl() {
		conversionService = new DefaultConversionService();
		conversionService.addConverter(new KvToCatalogArtistConverter());
		conversionService.addConverter(new KvToCatalogSongConverter());
		conversionService.addConverter(new KvToCatalogSongListConverter());
	}

	@Override
	@Cacheable("kvCatalogCache")
	public CatalogArtist getArtist(long artistId) {
		try {
			KvQuery<KvQuery.ArtistGet> query = new KvQuery<KvQuery.ArtistGet>()
					.setAffiliateId(properties.getAffiliateId())
					.setFunction("get")
					.setParameters(new KvQuery.ArtistGet().setId(artistId));

			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getEndpoint())
					.path("/artist/")
					.queryParam("query", jsonMapper.writeValueAsString(query));

			KvArtistResponse response = restTemplate.getForObject(builder.build().encode().toUri(), KvArtistResponse.class);
			return conversionService.convert(response.getArtist(), CatalogArtist.class);

		} catch (JsonProcessingException | RestClientException e) {
			throw new TechnicalException(e);
		}
	}

	@Override
	@Cacheable("kvCatalogCache")
	public CatalogSong getSong(long songId) {
		try {
			KvQuery<KvQuery.SongGet> query = new KvQuery<KvQuery.SongGet>()
					.setAffiliateId(properties.getAffiliateId())
					.setFunction("get")
					.setParameters(new KvQuery.SongGet().setId(songId));

			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getEndpoint())
					.path("/song/")
					.queryParam("query", jsonMapper.writeValueAsString(query));

			KvSongResponse response = restTemplate.getForObject(builder.build().encode().toUri(), KvSongResponse.class);
			return conversionService.convert(response.getSong(), CatalogSong.class);

		} catch (JsonProcessingException | RestClientException e) {
			throw new TechnicalException(e);
		}
	}

	@Override
	@Cacheable("kvCatalogCache")
	public CatalogSongList getSongList(CatalogSongListType type, String filter, Integer limit, Long offset) {
		try {
			String path;
			KvQuery<?> query;
			switch (type) {
			case query:
				path = "/search/";
				query = new KvQuery<KvQuery.SearchSong>()
						.setAffiliateId(properties.getAffiliateId())
						.setFunction("song")
						.setParameters(new KvQuery.SearchSong().setQuery(filter).setLimit(limit).setOffset(offset));
				break;
			case artist:
				path = "/song/";
				query = new KvQuery<KvQuery.SongList>()
						.setAffiliateId(properties.getAffiliateId())
						.setFunction("list")
						.setParameters(new KvQuery.SongList().setArtistId(Arrays.asList(Long.valueOf(filter))).setLimit(limit).setOffset(offset));
				break;
			default:
				throw new UnsupportedOperationException("Unsupported song list type '" + type + "'");
			}

			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(properties.getEndpoint())
					.path(path)
					.queryParam("query", jsonMapper.writeValueAsString(query));

			KvSongList response = restTemplate.getForObject(builder.build().encode().toUri(), KvSongList.class);
			return conversionService.convert(response, CatalogSongList.class).setType(type);

		} catch (JsonProcessingException | RestClientException e) {
			throw new TechnicalException(e);
		}
	}

	@Override
	public CatalogSelectionList getSelectionList(CatalogSelectionType type) {
		throw new UnsupportedOperationException();
	}

	public class KvToCatalogArtistConverter implements Converter<KvArtist, CatalogArtist> {

		@Override
		public CatalogArtist convert(KvArtist source) {
			return new CatalogArtist()
					.setId(source.getId())
					.setName(source.getName());
		}

	}

	public class KvToCatalogSongConverter implements Converter<KvSong, CatalogSong> {

		@Override
		public CatalogSong convert(KvSong source) {
			return new CatalogSong()
					.setId(source.getId())
					.setName(source.getName())
					.setArtist(new CatalogArtist().setId(source.getArtistId()))
					.setImg(source.getImgUrl());
		}

	}

	public class KvToCatalogSongListConverter implements Converter<KvSongList, CatalogSongList> {

		@Override
		public CatalogSongList convert(KvSongList source) {
			CatalogSongList target = new CatalogSongList()
					.setCount(source.getLength())
					.setTotal(source.getTotalLength());
			if (source.getSongs() != null) {
				target.setSongs(source.getSongs().stream()
						.map(it -> conversionService.convert(it, CatalogSong.class))
						.collect(Collectors.toCollection(LinkedHashSet::new)));
			}
			return target;
		}

	}

}