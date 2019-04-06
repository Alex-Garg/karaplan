package me.crespel.karaplan.domain;

import java.util.Calendar;
import java.util.Set;
import java.util.SortedSet;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.SortComparator;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(exclude = {"artist", "votes", "comments", "playlists"})
@ToString(of = {"id", "name"})
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "song")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Song {

	@Id
	@GeneratedValue
	@Column(name = "ID", unique = true)
	private Long id;

	@Column(name = "CATALOG_ID", unique = true)
	private Long catalogId;

	@NotNull
	@Column(name = "NAME")
	private String name;

	@Column(name = "DURATION")
	private Long duration;

	@Column(name = "IMAGE")
	private String image;

	@Lob
	@Type(type = "org.hibernate.type.TextType")
	@Column(name = "LYRICS")
	private String lyrics;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.MERGE })
	@JoinColumn(name = "FK_ARTIST", referencedColumnName = "ID")
	@JsonIgnoreProperties("songs")
	private Artist artist;

	@Column(name = "SCORE")
	private Integer score;

	@Column(name = "SCORE_UP")
	private Integer scoreUp;

	@Column(name = "SCORE_DOWN")
	private Integer scoreDown;

	@OneToMany(mappedBy = "song", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnoreProperties("song")
	@SortComparator(SongVote.OrderByIdDescComparator.class)
	@OrderBy("id DESC")
	private SortedSet<SongVote> votes = Sets.newTreeSet(SongVote.orderByIdDescComparator);

	@Column(name = "COMMENTS_COUNT")
	private Integer commentsCount;

	@OneToMany(mappedBy = "song", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
	@JsonIgnoreProperties("song")
	@SortComparator(SongComment.OrderByIdDescComparator.class)
	@OrderBy("id DESC")
	private SortedSet<SongComment> comments = Sets.newTreeSet(SongComment.orderByIdDescComparator);

	@Column(name = "PLAYLISTS_COUNT")
	private Integer playlistsCount;

	@ManyToMany(mappedBy = "songs", fetch = FetchType.EAGER)
	@JsonIgnoreProperties("songs")
	private Set<Playlist> playlists = Sets.newLinkedHashSet();

	@CreatedDate
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "CREATED_DATE")
	private Calendar createdDate;

	@CreatedBy
	@ManyToOne
	@JoinColumn(name = "FK_USER_CREATED", referencedColumnName = "ID")
	private User createdBy;

	@LastModifiedDate
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name = "UPDATED_DATE")
	private Calendar updatedDate;

	@LastModifiedBy
	@ManyToOne
	@JoinColumn(name = "FK_USER_UPDATED", referencedColumnName = "ID")
	private User updatedBy;

	public void updateStats() {
		this.score = (votes != null) ? votes.stream().mapToInt(SongVote::getScore).sum() : 0;
		this.scoreUp = (votes != null) ? votes.stream().mapToInt(SongVote::getScore).filter(score -> score > 0).sum() : 0;
		this.scoreDown = (votes != null) ? Math.abs(votes.stream().mapToInt(SongVote::getScore).filter(score -> score < 0).sum()) : 0;
		this.commentsCount = (comments != null) ? comments.size() : 0;
		this.playlistsCount = (playlists != null) ? playlists.size() : 0;
	}

}
