import { TestBed, ComponentFixture } from '@angular/core/testing';
import { UsersComponent } from './users.component';
import { UserService } from '../user.service';
import { of } from 'rxjs';
import User from '../user';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('UsersComponent', () => {
  let component: UsersComponent;
  let fixture: ComponentFixture<UsersComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;

  const mockUsers: User[] = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }];

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('UserService', ['getUsers', 'addUser', 'deleteUser']);
    spy.getUsers.and.returnValue(of(mockUsers));

    await TestBed.configureTestingModule({
      declarations: [UsersComponent],
      providers: [{ provide: UserService, useValue: spy }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    userServiceSpy = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
    fixture = TestBed.createComponent(UsersComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load users from service on init and render them as list items', () => {
    expect(component.users).toEqual(mockUsers);
    const items = fixture.nativeElement.querySelectorAll('ul.users li');
    expect(items.length).toBe(2);
  });

  it('should call addUser and append the new user when add() is called with a name', () => {
    const newUser: User = { id: 3, name: 'Carol' };
    userServiceSpy.addUser.and.returnValue(of(newUser));

    component.add('Carol');

    expect(userServiceSpy.addUser).toHaveBeenCalledWith({ name: 'Carol' } as User);
    expect(component.users).toContain(newUser);
  });

  it('should not call addUser when the name is blank or whitespace-only', () => {
    component.add('   ');
    expect(userServiceSpy.addUser).not.toHaveBeenCalled();
  });

  it('should remove user from local array and call deleteUser when delete() is called', () => {
    userServiceSpy.deleteUser.and.returnValue(of({} as User));
    const userToDelete = mockUsers[0];

    component.delete(userToDelete);

    expect(component.users).not.toContain(userToDelete);
    expect(userServiceSpy.deleteUser).toHaveBeenCalledWith(userToDelete.id!);
  });
});
