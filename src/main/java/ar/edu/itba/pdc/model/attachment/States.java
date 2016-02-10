package ar.edu.itba.pdc.model.attachment;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import ar.edu.itba.pdc.defs.Defs.ErrorType;
import ar.edu.itba.pdc.utils.BufferManager;

public enum States implements Processable {

	READING_FIRST_LINE {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			if (c == '\r') {
				attachmentHTTP.state = READING_FIRST_LINE_AFTER_CARRIAGE;
			} else if (c == '\n') {
				if (!attachmentHTTP.parseFirstLine(key)) {
					attachmentHTTP.state = IDLE;
				}
			} else {
				attachmentHTTP.buffers[0].append(c);
			}
		}

	},
	READING_FIRST_LINE_AFTER_CARRIAGE {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			if (c == '\n') {
				if (!attachmentHTTP.parseFirstLine(key)) {
					attachmentHTTP.state = IDLE;
				}
			} else {
				attachmentHTTP.reportError(key, ErrorType.BAD_REQUEST, "Bad Request");
				attachmentHTTP.state = IDLE;
			}
		}
	},
	READING_HEADER {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			attachmentHTTP.buffers[0].append(c);

			if (c == '\r') {
				attachmentHTTP.state = States.READING_HEADER_AFTER_CARRIAGE;
			} else if (c == '\n') {
				if (attachmentHTTP.buffers[0].length() == 1) {
					if (!attachmentHTTP.parseHeader(key)) {
						attachmentHTTP.state = IDLE;
					}
				} else {
					attachmentHTTP.state = READING_HEADER_NEW_LINE;
				}
			}
		}
	},
	READING_HEADER_AFTER_CARRIAGE {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			attachmentHTTP.buffers[0].append(c);
			if (c == '\n') {
				if (attachmentHTTP.buffers[0].length() == 2) {
					if (!attachmentHTTP.parseHeader(key)) {
						attachmentHTTP.state = IDLE;
						return;
					}
				} else {
					attachmentHTTP.state = READING_HEADER_NEW_LINE;
				}
			} else {
				attachmentHTTP.state = READING_HEADER;
			}
		}
	},
	READING_HEADER_NEW_LINE {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			if (c == ' ' || c == '\t') {
				attachmentHTTP.buffers[0].append(c);
				attachmentHTTP.state = READING_HEADER;
			} else {
				if (!attachmentHTTP.parseHeader(key)) {
					attachmentHTTP.state = IDLE;
					return;
				}
				attachmentHTTP.buffers[0] = new StringBuffer();
				attachmentHTTP.buffers[0].append(c);
				if (c == '\n') {
					if (!attachmentHTTP.parseHeader(key)){
						attachmentHTTP.state = IDLE;
					}
				} else if (c == '\r') {
					attachmentHTTP.state = READING_HEADER_NEW_LINE_AFTER_CARRIAGE;
				} else {
					attachmentHTTP.state = READING_HEADER;
				}
			}

		}
	},
	READING_HEADER_NEW_LINE_AFTER_CARRIAGE {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			if (c == '\n') {
				attachmentHTTP.buffers[0].append(c);
				if (!attachmentHTTP.parseHeader(key)){
					attachmentHTTP.state = IDLE;
				}
			} else {
				attachmentHTTP.reportError(key, ErrorType.BAD_REQUEST, "Bad Request");
				attachmentHTTP.state = IDLE;
			}
		}
	},
	READING_BODY_NORMAL {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			int amount = attachmentHTTP.parseBody(key);
			if (amount==-1){
				attachmentHTTP.state=IDLE;
			}else if(attachmentHTTP.leftLen != null) {
				attachmentHTTP.leftLen = attachmentHTTP.leftLen - amount;
				if (attachmentHTTP.leftLen <= 0) {
					attachmentHTTP.finishedReading(key);
					attachmentHTTP.state = IDLE;
				}
			}
		}
	},
	READING_BODY_MULTIPART {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			if (!attachmentHTTP.parseBodyMultipart(key)){
				attachmentHTTP.state=IDLE;
			}
		}
	},
	READING_BODY_CHUNKED_FIRST_LINE {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			if (c == '\r') {
				attachmentHTTP.state = READING_BODY_CHUNKED_FIRST_LINE_AFTER_CARRIAGE;
			} else if (c == '\n') {
				if (!attachmentHTTP.parseFirstLineOfChunk(key)){
					attachmentHTTP.state = IDLE;
				}
			} else {
				attachmentHTTP.buffers[0].append(c);
			}
		}

	},
	READING_BODY_CHUNKED_FIRST_LINE_AFTER_CARRIAGE {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			if (c == '\n') {
				if (!attachmentHTTP.parseFirstLineOfChunk(key)){
					attachmentHTTP.state=IDLE;
				}				
			} else {
				attachmentHTTP.buffers[0].append(c);
				attachmentHTTP.state = READING_BODY_CHUNKED_FIRST_LINE;
			}
		}
	},
	READING_BODY_CHUNKED_BODY {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			int amount = attachmentHTTP.parseBody(key);
			if (amount==-1){
				attachmentHTTP.state=IDLE;
			}else if (attachmentHTTP.leftLen != null) {
				attachmentHTTP.leftLen = attachmentHTTP.leftLen - amount;
				if (attachmentHTTP.leftLen <= 0) {
					attachmentHTTP.state = READING_BODY_CHUNKED_END_OF_CHUNK;
				}
			}

		}

	},
	READING_BODY_CHUNKED_END_OF_CHUNK {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			BufferManager.addCharacterToBuffer(c,
					attachmentHTTP.writeBufferWrapper);
			if (c == '\r') {
				attachmentHTTP.state = READING_BODY_CHUNKED_END_OF_CHUNK_AFTER_CARRIAGE;
			} else if (c == '\n') {
				attachmentHTTP.state = READING_BODY_CHUNKED_FIRST_LINE;
			}

		}
	},
	READING_BODY_CHUNKED_END_OF_CHUNK_AFTER_CARRIAGE {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			BufferManager.addCharacterToBuffer(c,
					attachmentHTTP.writeBufferWrapper);
			if (c == '\n') {
				attachmentHTTP.state = READING_BODY_CHUNKED_FIRST_LINE;
			}
		}
	},
	READING_BODY_CHUNKED_TRAILER {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			BufferManager.addCharacterToBuffer(c,
					attachmentHTTP.writeBufferWrapper);
			if (c == '\r') {
				if (attachmentHTTP.buffers[1].length() > 0)
					attachmentHTTP.state = READING_BODY_CHUNKED_TRAILER_AFTER_CARRIAGE;
				else
					attachmentHTTP.state = READING_BODY_CHUNKED_POSSIBLE_END;
			} else if (c == '\n') {
				if (attachmentHTTP.buffers[1].length() > 0)
					attachmentHTTP.state = READING_BODY_CHUNKED_POSSIBLE_END;
				else {
					attachmentHTTP.finishedReading(key);
					attachmentHTTP.state = IDLE;
				}

			} else {
				attachmentHTTP.buffers[1].append(c);
			}
		}
	},
	READING_BODY_CHUNKED_TRAILER_AFTER_CARRIAGE {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			BufferManager.addCharacterToBuffer(c,
					attachmentHTTP.writeBufferWrapper);
			if (c == '\n') {
				attachmentHTTP.state = READING_BODY_CHUNKED_POSSIBLE_END;
			} else {
				attachmentHTTP.state = READING_BODY_CHUNKED_TRAILER;
			}
		}
	},
	READING_BODY_CHUNKED_POSSIBLE_END {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			BufferManager.addCharacterToBuffer(c,
					attachmentHTTP.writeBufferWrapper);
			if (c == '\r') {
				attachmentHTTP.state = READING_BODY_CHUNKED_POSSIBLE_END_AFTER_CARRIAGE;
			} else if (c == '\n') {
				attachmentHTTP.finishedReading(key);
				attachmentHTTP.state = IDLE;
			}
		}
	},
	READING_BODY_CHUNKED_POSSIBLE_END_AFTER_CARRIAGE {
		public void process(SelectionKey key) throws IOException {
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			Character c = BufferManager.getCharacterFromBuffer(attachmentHTTP
					.getReadBuffer());
			BufferManager.addCharacterToBuffer(c,
					attachmentHTTP.writeBufferWrapper);
			if (c == '\n') {
				attachmentHTTP.finishedReading(key);
				attachmentHTTP.state = IDLE;
			} else {
				attachmentHTTP.state = READING_BODY_CHUNKED_TRAILER;
			}

		}
	},
	IDLE {
		public void process(SelectionKey key) throws IOException {
			// Consume el caracter pero no hace nada
			AttachmentHTTP attachmentHTTP = (AttachmentHTTP) key.attachment();
			BufferManager
					.getCharacterFromBuffer(attachmentHTTP.getReadBuffer());
		}
	};

}
