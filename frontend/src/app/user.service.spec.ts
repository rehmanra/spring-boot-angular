import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { UserService } from './user.service';
import User from './user';
import { environment } from '../environments/environment';

describe('UserService', () => {
  let service: UserService;
  let httpMock: HttpTestingController;
  const baseUrl = environment.apiEndpoint + 'user/';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(UserService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getUsers', () => {
    it('should issue GET to the users endpoint and return results', () => {
      const mockUsers: User[] = [{ id: 1, name: 'Alice' }, { id: 2, name: 'Bob' }];

      service.getUsers().subscribe(users => expect(users).toEqual(mockUsers));

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockUsers);
    });

    it('should return an empty array when the server errors', () => {
      service.getUsers().subscribe(users => expect(users).toEqual([]));

      httpMock.expectOne(baseUrl).flush('Server Error', { status: 500, statusText: 'Server Error' });
    });
  });

  describe('getUser', () => {
    it('should issue GET to the user-by-id endpoint', () => {
      const mockUser: User = { id: 1, name: 'Alice' };

      service.getUser(1).subscribe(user => expect(user).toEqual(mockUser));

      const req = httpMock.expectOne(`${baseUrl}1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);
    });
  });

  describe('searchUsers', () => {
    it('should issue GET with name param when term is non-blank', () => {
      const mockUsers: User[] = [{ id: 1, name: 'Alice' }];

      service.searchUsers('Ali').subscribe(users => expect(users).toEqual(mockUsers));

      const req = httpMock.expectOne(`${baseUrl}?name=Ali`);
      expect(req.request.method).toBe('GET');
      req.flush(mockUsers);
    });

    it('should return empty array and make no HTTP call for a blank term', () => {
      let result: User[] = [{ id: 99, name: 'sentinel' }];

      service.searchUsers('   ').subscribe(users => result = users);

      httpMock.expectNone(baseUrl);
      expect(result).toEqual([]);
    });
  });

  describe('addUser', () => {
    it('should issue POST with user body and return saved user', () => {
      const newUser: User = { name: 'Carol' } as User;
      const savedUser: User = { id: 3, name: 'Carol' };

      service.addUser(newUser).subscribe(user => expect(user).toEqual(savedUser));

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newUser);
      req.flush(savedUser);
    });
  });

  describe('updateUser', () => {
    it('should issue PUT with user body', () => {
      const user: User = { id: 1, name: 'Updated' };

      service.updateUser(user).subscribe();

      const req = httpMock.expectOne(baseUrl);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(user);
      req.flush({});
    });
  });

  describe('deleteUser', () => {
    it('should issue DELETE to the correct user endpoint without double-slash', () => {
      service.deleteUser(1).subscribe();

      const req = httpMock.expectOne(`${baseUrl}1`);
      expect(req.request.method).toBe('DELETE');
      req.flush({});
    });
  });
});
