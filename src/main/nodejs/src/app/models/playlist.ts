import { PlaylistSong } from './playlist-song';
import { User } from './user';

export class Playlist {
  id: number;
  name: string;
  restricted: boolean;
  accessKey: string;
  members: User[];
  songsCount: number;
  songs: PlaylistSong[];
  duration: number;
  createdDate: Date;
  createdBy: User;
  updatedDate: Date;
  updatedBy: User;
  isSelected: boolean;
  readOnly: boolean;
}
