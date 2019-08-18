import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';
import { PlaylistsService } from '../services/playlists.service';
import { PlaylistModalComponent } from '../playlist-modal/playlist-modal.component';
import { Playlist } from '../models/playlist';

@Component({
  selector: 'app-playlists',
  templateUrl: './playlists.component.html',
  styleUrls: ['./playlists.component.css']
})
export class PlaylistsComponent implements OnInit {

  playlists: Playlist[] = null;

  constructor(
    private router: Router,
    private modalService: NgbModal,
    private playlistsService: PlaylistsService
  ) { }

  ngOnInit() {
    this.refreshPlaylists();
  }

  refreshPlaylists() {
    this.playlistsService.getPlaylists(0, 100, 'name').subscribe(playlists => {
      this.playlists = playlists;
    });
  }

  trackByPlaylistId(index: number, playlist: Playlist): number {
    return playlist.id;
  }

  gotoPlaylist(playlist) {
    this.router.navigate(['/playlists', playlist.id]);
  }

  createPlaylist() {
    let modal = this.modalService.open(PlaylistModalComponent);
    modal.componentInstance.playlist = new Playlist();
    modal.result.then((result: Playlist) => {
      this.playlistsService.createPlaylist(result.name).subscribe(playlist => {
        this.gotoPlaylist(playlist);
      });
    }, reason => {});
  }

  editPlaylist(playlist: Playlist) {
    let modal = this.modalService.open(PlaylistModalComponent);
    modal.componentInstance.playlist = new Playlist(playlist.id, playlist.name, playlist.readOnly)
    modal.result.then((result: Playlist) => {
      this.playlistsService.savePlaylist(result).subscribe(playlist => {
        this.refreshPlaylists();
      });
    }, reason => {});
  }

  leavePlaylist(playlist: Playlist) {
    this.playlistsService.leavePlaylist(playlist.id).subscribe(response => {
      this.refreshPlaylists();
    });
  }

}
