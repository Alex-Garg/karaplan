import { Component, OnInit, Input } from '@angular/core';
import { AccountService } from '../services/account.service';
import { SongsService } from '../services/songs.service';
import { User } from '../models/user';
import { Song } from '../models/song';
import { Playlist } from '../models/playlist';
import { PlaylistsService } from '../services/playlists.service';

@Component({
  selector: 'app-song-actions',
  templateUrl: './song-actions.component.html',
  styleUrls: ['./song-actions.component.css']
})
export class SongActionsComponent implements OnInit {

  @Input() song: Song;
  @Input() showVotes: boolean = true;
  @Input() showComments: boolean = true;
  @Input() showPlaylists: boolean = true;

  user: User = null;
  playlists: Playlist[] = null;
  commentText: string;

  constructor(
    private accountService: AccountService,
    private songsService: SongsService,
    private playlistsService: PlaylistsService
  ) { }

  ngOnInit() {
    this.accountService.getPrincipal().subscribe(principal => {
      this.user = principal.user;
    });
  }

  voteUp() {
    this.songsService.voteSongByCatalogId(this.song.catalogId, 1).subscribe(song => {
      this.song = song;
    });
  }

  voteDown() {
    this.songsService.voteSongByCatalogId(this.song.catalogId, -1).subscribe(song => {
      this.song = song;
    });
  }

  addComment(comment: string) {
    this.songsService.addCommentToSongByCatalogId(this.song.catalogId, comment).subscribe(song => {
      this.song = song;
      this.commentText = '';
    });
  }

  removeComment(commentId: number) {
    this.songsService.removeCommentFromSongByCatalogId(this.song.catalogId, commentId).subscribe(song => {
      this.song = song;
    });
  }

  addToPlaylist(playlist: Playlist) {
    this.songsService.addSongToPlaylistByCatalogId(this.song.catalogId, playlist.id).subscribe(song => {
      this.song = song;
      playlist.isSelected = true;
    });
  }

  removeFromPlaylist(playlist: Playlist) {
    this.songsService.removeSongFromPlaylistByCatalogId(this.song.catalogId, playlist.id).subscribe(song => {
      this.song = song;
      playlist.isSelected = false;
    });
  }

  togglePlaylist(playlist: Playlist) {
    if (playlist.isSelected) {
      this.removeFromPlaylist(playlist);
    } else {
      this.addToPlaylist(playlist);
    }
  }

  onPlaylistOpen() {
    if (this.playlists == null) {
      this.playlistsService.getPlaylists().subscribe(playlists => {
        playlists.forEach(playlist => {
          playlist.isSelected = (this.song.playlists && this.song.playlists.findIndex(songPlaylist => songPlaylist.id == playlist.id) >= 0);
        });
        this.playlists = playlists;
      });
    }
  }

}
