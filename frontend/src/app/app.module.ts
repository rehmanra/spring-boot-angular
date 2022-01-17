import {NgModule} from '@angular/core';
import {HttpClientModule} from '@angular/common/http';
import {BrowserModule} from '@angular/platform-browser';
import {FormsModule} from '@angular/forms';
import {AppRoutingModule} from './app-routing.module';

import {MessagesComponent} from './messages/messages.component';
import {AppComponent} from './app.component';
import {DashboardComponent} from "./dashboard/dashboard.component";
import {UserDetailComponent} from './user-detail/user-detail.component';
import {UsersComponent} from './users/users.component';
import {UserSearchComponent} from './user-search/user-search.component';

@NgModule({
  declarations: [
    AppComponent,
    DashboardComponent,
    UsersComponent,
    UserDetailComponent,
    MessagesComponent,
    UserSearchComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    AppRoutingModule,
    HttpClientModule
  ],
  exports: [
    MessagesComponent
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {
}
