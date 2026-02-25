import { TestBed } from '@angular/core/testing';
import { MessageService } from './message.service';

describe('MessageService', () => {
  let service: MessageService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MessageService);
  });

  it('should be created with an empty messages array', () => {
    expect(service).toBeTruthy();
    expect(service.messages).toEqual([]);
  });

  describe('add', () => {
    it('should add a single message', () => {
      service.add('hello');
      expect(service.messages).toEqual(['hello']);
    });

    it('should accumulate multiple messages in order', () => {
      service.add('first');
      service.add('second');
      expect(service.messages).toHaveSize(2);
      expect(service.messages[0]).toBe('first');
      expect(service.messages[1]).toBe('second');
    });
  });

  describe('clear', () => {
    it('should remove all messages', () => {
      service.add('a');
      service.add('b');
      service.clear();
      expect(service.messages).toEqual([]);
    });

    it('should be a no-op when messages are already empty', () => {
      service.clear();
      expect(service.messages).toEqual([]);
    });
  });
});
