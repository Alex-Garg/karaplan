package me.crespel.karaplan.model.kv;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class KvSongList {

	private Long totalLength;
	private Long length;
	private List<KvSong> songs;

}
