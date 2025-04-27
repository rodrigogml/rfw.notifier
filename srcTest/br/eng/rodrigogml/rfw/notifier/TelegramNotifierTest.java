package br.eng.rodrigogml.rfw.notifier;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Classe de teste para a {@link TelegramNotifier}. Lê configurações sensíveis de um arquivo externo testconfig.properties.
 */
public class TelegramNotifierTest {

  private static String TOKEN;
  private static String CHAT_ID;

  @BeforeClass
  public static void loadConfig() throws IOException {
    Properties properties = new Properties();
    try (InputStream fis = TelegramNotifier.class.getResourceAsStream("/testconfig.properties")) {
      properties.load(fis);
      TOKEN = properties.getProperty("TOKEN");
      CHAT_ID = properties.getProperty("CHAT_ID");
    }
  }

  @Test
  public void testSendMessage() throws IOException {
    TelegramNotifier.sendMessage(TOKEN, CHAT_ID, "Mensagem de teste enviada pelo bot via InputStream.");
  }

  @Test
  public void testSendDocument() throws IOException {
    try (InputStream document = TelegramNotifier.class.getResourceAsStream("/resources/arquivo.pdf")) {
      TelegramNotifier.sendDocument(TOKEN, CHAT_ID, document, "arquivo.pdf");
    }
  }

  @Test
  public void testSendPhoto() throws IOException {
    try (InputStream photo = TelegramNotifier.class.getResourceAsStream("/resources/image.png")) {
      TelegramNotifier.sendPhoto(TOKEN, CHAT_ID, photo, "image.png");
    }
  }

  @Test
  public void testSendAudio() throws IOException {
    try (InputStream audio = TelegramNotifier.class.getResourceAsStream("/resources/audio.mp3")) {
      TelegramNotifier.sendAudio(TOKEN, CHAT_ID, audio, "audio.mp3");
    }
  }

  @Test
  public void testSendVideo() throws IOException {
    try (InputStream video = TelegramNotifier.class.getResourceAsStream("/resources/video.mp4")) {
      TelegramNotifier.sendVideo(TOKEN, CHAT_ID, video, "video.mp4");
    }
  }
}
