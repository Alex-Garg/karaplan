import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { shareReplay } from 'rxjs/operators';
import { Principal } from '../models/principal';
import { User } from '../models/user';

@Injectable({
  providedIn: 'root'
})
export class AccountService {
  private accountUrl = 'api/v1/account';
  private principal$ = null;

  constructor(
    private http: HttpClient
  ) { }

  getPrincipal(cache: boolean = true): Observable<Principal> {
    const url = `${this.accountUrl}/principal`;
    if (!cache) {
      return this.http.get<Principal>(url);
    } else if (this.principal$ == null) {
      this.principal$ = this.http.get<Principal>(url).pipe(shareReplay(1));
    }
    return this.principal$;
  }

  updateUser(user: User): Observable<User> {
    const url = `${this.accountUrl}/user`;
    return this.http.post<User>(url, user);
  }
}
