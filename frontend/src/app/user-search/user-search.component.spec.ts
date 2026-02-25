import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { UserSearchComponent } from './user-search.component';
import { UserService } from '../user.service';
import { of } from 'rxjs';
import User from '../user';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('UserSearchComponent', () => {
  let component: UserSearchComponent;
  let fixture: ComponentFixture<UserSearchComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj('UserService', ['searchUsers']);
    userServiceSpy.searchUsers.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      declarations: [UserSearchComponent],
      providers: [{ provide: UserService, useValue: userServiceSpy }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(UserSearchComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialise users$ observable on init', () => {
    expect(component.users$).toBeTruthy();
  });

  it('should call searchUsers after the 300ms debounce elapses', fakeAsync(() => {
    const users: User[] = [{ id: 1, name: 'Alice' }];
    userServiceSpy.searchUsers.and.returnValue(of(users));

    component.search('Ali');
    tick(300);

    expect(userServiceSpy.searchUsers).toHaveBeenCalledWith('Ali');
  }));

  it('should NOT call searchUsers before the debounce time has elapsed', fakeAsync(() => {
    component.search('quick');
    tick(100);
    expect(userServiceSpy.searchUsers).not.toHaveBeenCalled();
    tick(200); // allow timer to complete cleanly
  }));

  it('should NOT call searchUsers a second time for the same consecutive term', fakeAsync(() => {
    userServiceSpy.searchUsers.and.returnValue(of([]));

    component.search('test');
    tick(300);
    component.search('test'); // identical — distinctUntilChanged suppresses this
    tick(300);

    expect(userServiceSpy.searchUsers).toHaveBeenCalledTimes(1);
  }));
});
