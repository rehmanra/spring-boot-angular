import User from './user';

describe('User', () => {
  it('should create an instance', () => {
    const user: User = { id: 1, name: 'test' };
    expect(user).toBeTruthy();
  });

  it('should hold the correct values', () => {
    const user: User = { id: 42, name: 'Alice' };
    expect(user.id).toBe(42);
    expect(user.name).toBe('Alice');
  });
});
