import { TestBed, ComponentFixture } from '@angular/core/testing';
import { MessagesComponent } from './messages.component';
import { MessageService } from '../message.service';

describe('MessagesComponent', () => {
  let component: MessagesComponent;
  let fixture: ComponentFixture<MessagesComponent>;
  let messageService: MessageService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [MessagesComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(MessagesComponent);
    component = fixture.componentInstance;
    messageService = TestBed.inject(MessageService);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not render message container when there are no messages', () => {
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('h2')).toBeNull();
  });

  it('should render Messages header and message text when messages exist', () => {
    messageService.add('test message');
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('h2')?.textContent).toContain('Messages');
    expect(el.textContent).toContain('test message');
  });

  it('should clear all messages and hide container when clear button is clicked', () => {
    messageService.add('to be cleared');
    fixture.detectChanges();
    const button = fixture.nativeElement.querySelector('button.clear') as HTMLButtonElement;
    button.click();
    fixture.detectChanges();
    expect(messageService.messages).toEqual([]);
    expect(fixture.nativeElement.querySelector('h2')).toBeNull();
  });
});
