import { TestBed, ComponentFixture } from '@angular/core/testing';
import { UserDetailComponent } from './user-detail.component';
import { UserService } from '../user.service';
import { ActivatedRoute } from '@angular/router';
import { Location } from '@angular/common';
import { of } from 'rxjs';
import User from '../user';
import { FormsModule } from '@angular/forms';
import { NO_ERRORS_SCHEMA } from '@angular/core';

describe('UserDetailComponent', () => {
  let component: UserDetailComponent;
  let fixture: ComponentFixture<UserDetailComponent>;
  let userServiceSpy: jasmine.SpyObj<UserService>;
  let locationSpy: jasmine.SpyObj<Location>;

  const mockUser: User = { id: 1, name: 'Alice' };

  beforeEach(async () => {
    userServiceSpy = jasmine.createSpyObj('UserService', ['getUser', 'updateUser']);
    locationSpy = jasmine.createSpyObj('Location', ['back']);
    userServiceSpy.getUser.and.returnValue(of(mockUser));

    await TestBed.configureTestingModule({
      declarations: [UserDetailComponent],
      imports: [FormsModule],
      providers: [
        { provide: UserService, useValue: userServiceSpy },
        { provide: Location, useValue: locationSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => '1' } } } }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(UserDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load user by id from route on init', () => {
    expect(component.user).toEqual(mockUser);
    expect(userServiceSpy.getUser).toHaveBeenCalledWith(1);
  });

  it('should render user name in uppercase in the heading', () => {
    const h2 = fixture.nativeElement.querySelector('h2') as HTMLElement;
    expect(h2.textContent).toContain('ALICE');
  });

  it('should call location.back() when goBack() is called', () => {
    component.goBack();
    expect(locationSpy.back).toHaveBeenCalled();
  });

  it('should call updateUser and then navigate back when save() is called', () => {
    userServiceSpy.updateUser.and.returnValue(of({}));
    component.save();
    expect(userServiceSpy.updateUser).toHaveBeenCalledWith(mockUser);
    expect(locationSpy.back).toHaveBeenCalled();
  });

  it('should not call updateUser when user is undefined', () => {
    component.user = undefined;
    component.save();
    expect(userServiceSpy.updateUser).not.toHaveBeenCalled();
  });
});
