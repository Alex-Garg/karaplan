package me.crespel.karaplan.model.kv;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class KvArtistResponse {

	private KvArtist artist;

}
