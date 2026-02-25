import { TestBed, ComponentFixture } from '@angular/core/testing';
import { DashboardComponent } from './dashboard.component';
import { UserService } from '../user.service';
import { of } from 'rxjs';
import User from '../user';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;

  beforeEach(async () => {
    const spy = jasmine.createSpyObj('UserService', ['getUsers']);

    await TestBed.configureTestingModule({
      declarations: [DashboardComponent],
      providers: [{ provide: UserService, useValue: spy }],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    userServiceSpy = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
  });

  it('should create', () => {
    userServiceSpy.getUsers.and.returnValue(of([]));
    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should slice the result to at most 5 users', () => {
    const sixUsers: User[] = Array.from({ length: 6 }, (_, i) => ({ id: i + 1, name: `User${i + 1}` }));
    userServiceSpy.getUsers.and.returnValue(of(sixUsers));

    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance.users.length).toBe(5);
  });

  it('should render each user name in the users-menu', () => {
    const users: User[] = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }];
    userServiceSpy.getUsers.and.returnValue(of(users));

    fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();

    const links = fixture.nativeElement.querySelectorAll('.users-menu a') as NodeListOf<HTMLElement>;
    expect(links.length).toBe(2);
    expect(links[0].textContent?.trim()).toBe('Alice');
    expect(links[1].textContent?.trim()).toBe('Bob');
  });
});
