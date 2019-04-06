import { Component, OnInit } from '@angular/core';
import { SongsService } from '../services/songs.service';
import { Song } from '../models/song';

@Component({
  selector: 'app-songs',
  templateUrl: './songs.component.html',
  styleUrls: ['./songs.component.css']
})
export class SongsComponent implements OnInit {

  songs: Song[] = [];

  constructor(
    private songsService: SongsService
  ) { }

  ngOnInit() {
  }

  search(query: string) {
    this.songsService.search(query).subscribe(songs => {
      this.songs = songs;
    });
  }

}
