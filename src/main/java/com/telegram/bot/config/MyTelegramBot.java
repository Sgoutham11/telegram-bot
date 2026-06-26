package com.telegram.bot.config;

import com.telegram.bot.entity.GameSession;
import com.telegram.bot.entity.Player;
import com.telegram.bot.entity.TelegramBot;
import com.telegram.bot.exception.RecordNotFoundException;
import com.telegram.bot.repository.PlayerBoardRepository;
import com.telegram.bot.repository.TelegramBotRepository;
import com.telegram.bot.service.*;
import com.telegram.bot.utils.BingoEnums;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MyTelegramBot extends TelegramLongPollingBot {

    @Value("${telegram.bot1.username}")
    private String botUsername;

    @Autowired
    TelegramBotRepository telegramBotRepository;

    @Autowired
    PlayerBoardRepository playerBoardRepo;

    @Autowired
    private TelegramBotService bingoService;

    @Autowired
    private BingoCommandHandler bingoHandler;
    @Autowired
    private BingoGameService bingoGameService;
    @Autowired
    private BingoTurnService bingoTurnService;

    public static Long currentChatId = null;
    public static Long adminChatId = 658878520L;

    public MyTelegramBot(@Value("${telegram.bot1.token}") String botToken) {
        super(botToken);
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    public String saveNewChatId(Message message) {
        if (message == null || message.getText() == null)
            return "UNKNOWN";
        String sender;
        String text = message.getText();
        Long chatId = message.getChatId();
        Optional<TelegramBot> temp = telegramBotRepository.findById(chatId);
        if (temp.isEmpty()) {
            sender = extractNicNameFromChat(text);
            telegramBotRepository.save(new TelegramBot(chatId, message.getFrom().getUserName(), sender));
        } else if (text != null && text.toLowerCase().startsWith("update:")) {
            TelegramBot telegram = temp.get();
            sender = telegram.getNicName();
            telegram.setNicName(text.substring(text.indexOf(":") + 1));
            telegramBotRepository.save(telegram);
        } else {
            sender = temp.get().getNicName();
        }

        return sender;

    }


//    @Override
//    public void onUpdateReceived(Update update) {
//
//        try {
//
////            if (!update.hasMessage()||!update.hasCallbackQuery())
////                throw new RecordNotFoundException("No message found!!!!");
//
//            findTheMessageTypeAndDoAction(update);
//
//
//        } catch (RecordNotFoundException e) {
//            log.info("Custom error---:=>{}", e.getMessage());
//            sendMessage(new SendMessage(adminChatId.toString(), e.getMessage()));
//            currentChatId = null;
//        } catch (Exception e) {
//            sendMessage(new SendMessage(adminChatId.toString(), "Error!!!!"));
//            currentChatId = null;
//            log.error("error---:=>", e);
//        }
//
//
//    }

    public void findTheMessageTypeAndDoAction(Update update) {
        String sender;

        Message message = update.getMessage();
        sender = saveNewChatId(message);
//        log.info("message details{}", message);
        if (update.hasCallbackQuery()) handleCallbackQuery(update.getCallbackQuery());
        else if (message.hasText()) handleTextMessage(message, sender);
        else if (message.hasPhoto()) handlePhoto(message, sender);
        else if (message.hasDocument()) handleDocument(message, sender);
        else if (message.hasVideo()) handleVideo(message, sender);
        else if (message.hasAudio()) handleAudio(message, sender);
        else if (message.hasVoice()) handleVoice(message, sender);
        else if (message.hasVideoNote()) handleVideoNote(message, sender);
        else if (message.hasAnimation()) handleAnimation(message, sender);
        else if (message.hasSticker()) handleSticker(message, sender);
        else if (message.hasLocation()) handleLocation(message, sender);
        else if (message.hasContact()) handleContact(message, sender);
        else if (message.hasPoll()) handlePoll(message, sender);
        else if (message.hasDice()) handleDice(message, sender);
        else throw new RecordNotFoundException("Unknown chat type");

    }

    public void handlePhoto(Message message, String sender) {
        SendPhoto sendPhoto = new SendPhoto();

        try {
            List<PhotoSize> photos = message.getPhoto();
            String fileId = photos.get(photos.size() - 1).getFileId();
            String caption = message.getCaption();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the photo pls create the chat connection");
                sendPhoto.setChatId(currentChatId.toString());
                sendPhoto.setPhoto(new InputFile(fileId));
                if (caption != null)
                    sendPhoto.setCaption(caption);

                execute(sendPhoto);


            } else {
                sendPhoto.setChatId(adminChatId.toString());
                sendPhoto.setPhoto(new InputFile(fileId));
                if (caption != null)
                    sendPhoto.setCaption(sender + ":" + caption);

                execute(sendPhoto);
            }


        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }


    }

    public void handleDocument(Message message, String sender) {
        SendDocument sendDocument = new SendDocument();

        try {
            String fileId = message.getDocument().getFileId();
            String caption = message.getCaption();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the document pls create a chat connection");
                sendDocument.setChatId(currentChatId.toString());
                sendDocument.setDocument(new InputFile(fileId));
                if (caption != null)
                    sendDocument.setCaption(caption);
                execute(sendDocument);

            } else {
                sendDocument.setChatId(adminChatId.toString());
                sendDocument.setDocument(new InputFile(fileId));
                if (caption != null)
                    sendDocument.setCaption(sender + ":" + caption);

                execute(sendDocument);
            }


        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }

    }

    public Long getChatId(String userNameOrNicName) {
        return telegramBotRepository.getChatId(userNameOrNicName);
    }

    public String extractNicNameFromChat(String text) {
        String userId = "";

        Pattern pattern = Pattern.compile("^@(\\w+):");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            userId = matcher.group(1);

        }
        return userId;
    }

    public String extractMessageFromChat(String text) {
        String msg = "";

        Pattern pattern = Pattern.compile("^@\\w+:\\s*(.*)");
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            msg = matcher.group(1);

        }
        return msg;
    }

    private void handleTextMessage(Message message, String sender) {
        String text = message.getText();
        SendMessage msg = new SendMessage();

        if (text.equals("/bingo")) {
            startBingoGame(message);
            return;
        }
        if (message.getChatId().equals(adminChatId)) {
            if (text.startsWith("/start")) {
                msg.setChatId(adminChatId);
                msg.setText("Sent the name of user to establish a new connection like @goutham:");
            } else if (text.startsWith("@")) {
                currentChatId = getChatId(extractNicNameFromChat(text));
                if (currentChatId != null)
                    msg.setText("Chat connection initiated  with " + extractNicNameFromChat(text));

                else {
                    msg.setText("Failed to initiate chat connection with " + extractNicNameFromChat(text));
                }
                msg.setChatId(adminChatId);


                log.info("Chat id :{} MSG: {}", currentChatId, text);
                log.info("Message is Redirecting to {} : {}", extractNicNameFromChat(text), extractMessageFromChat(text));
            } else if (text.toLowerCase().startsWith("/stop")) {
                msg.setText("Chat connection stopped");
                msg.setChatId(adminChatId);
                currentChatId = null;
            } else {
                msg.setText(currentChatId != null ? message.getText() : "No connection !!!");
                msg.setChatId(currentChatId != null ? currentChatId : adminChatId);
            }
        } else {
            msg.setChatId(adminChatId);
            msg.setText(sender + ":" + message.getText());
        }

        sendMessage(msg);


    }


    private void sendMessage(SendMessage msg) {
//        SendMessage msg = new SendMessage(chatId.toString(), text);
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            log.error("Error----:=>", e);
            throw new RecordNotFoundException("Error!!!!!!!!");
        }
    }

    public void handleVideo(Message message, String sender) {
        SendVideo sendVideo = new SendVideo();
        try {
            String fileId = message.getVideo().getFileId();
            String caption = message.getCaption();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the video pls create the chat connection");
                sendVideo.setChatId(currentChatId.toString());
                sendVideo.setVideo(new InputFile(fileId));
                sendVideo.setCaption(caption != null ? caption : "");
                execute(sendVideo);

            } else {
                sendVideo.setChatId(adminChatId.toString());
                sendVideo.setVideo(new InputFile(fileId));
                if (caption != null)
                    sendVideo.setCaption(sender + ":" + caption);
                execute(sendVideo);
            }
        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }
    }

    public void handleAudio(Message message, String sender) {
        SendAudio sendAudio = new SendAudio();
        try {
            String fileId = message.getAudio().getFileId();
            String caption = message.getCaption();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the audio pls create the chat connection");
                sendAudio.setChatId(currentChatId.toString());
                sendAudio.setAudio(new InputFile(fileId));
                if (caption != null)
                    sendAudio.setCaption(caption);
                execute(sendAudio);

            } else {
                sendAudio.setChatId(adminChatId.toString());
                sendAudio.setAudio(new InputFile(fileId));
                if (caption != null)
                    sendAudio.setCaption(sender + ":" + caption);
                execute(sendAudio);
            }
        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }
    }

    public void handleVoice(Message message, String sender) {
        SendVoice sendVoice = new SendVoice();
        try {
            String fileId = message.getVoice().getFileId();
            String caption = message.getCaption();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the voice pls create a chat connection");
                sendVoice.setChatId(currentChatId.toString());
                sendVoice.setVoice(new InputFile(fileId));
                if (caption != null)
                    sendVoice.setCaption(caption);
                execute(sendVoice);

            } else {
                sendVoice.setChatId(adminChatId.toString());
                sendVoice.setVoice(new InputFile(fileId));
                if (caption != null)
                    sendVoice.setCaption(sender + ":" + caption);
                execute(sendVoice);
            }
        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }
    }

    public void handleVideoNote(Message message, String sender) {
        SendVideoNote sendVideoNote = new SendVideoNote();
        try {
            String fileId = message.getVideoNote().getFileId();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the video note pls create a chat connection");
                sendVideoNote.setChatId(currentChatId.toString());
                sendVideoNote.setVideoNote(new InputFile(fileId));
                execute(sendVideoNote);
            } else {
                sendVideoNote.setChatId(adminChatId.toString());
                sendVideoNote.setVideoNote(new InputFile(fileId));
                execute(sendVideoNote);
                sendMessage(new SendMessage(adminChatId.toString(), sender + ": sent a video note"));
            }
        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }
    }

    public void handleAnimation(Message message, String sender) {
        SendAnimation sendAnimation = new SendAnimation();
        try {
            String fileId = message.getAnimation().getFileId();
            String caption = message.getCaption();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the GIF pls create a chat connection");
                sendAnimation.setChatId(currentChatId.toString());
                sendAnimation.setAnimation(new InputFile(fileId));
                if (caption != null)
                    sendAnimation.setCaption(caption);
                execute(sendAnimation);

            } else {
                sendAnimation.setChatId(adminChatId.toString());
                sendAnimation.setAnimation(new InputFile(fileId));
                if (caption != null)
                    sendAnimation.setCaption(sender + ":" + caption);
                execute(sendAnimation);
            }
        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }
    }

    public void handleSticker(Message message, String sender) {
        SendSticker sendSticker = new SendSticker();
        try {
            String fileId = message.getSticker().getFileId();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the sticker pls create the chat connection");
                sendSticker.setChatId(currentChatId.toString());
                sendSticker.setSticker(new InputFile(fileId));
                execute(sendSticker);

            } else {
                sendSticker.setChatId(adminChatId.toString());
                sendSticker.setSticker(new InputFile(fileId));
                execute(sendSticker);
                sendMessage(new SendMessage(adminChatId.toString(), sender + ": sent a sticker"));
            }
        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }
    }

    public void handleLocation(Message message, String sender) {
        SendLocation sendLocation = new SendLocation();
        try {
            Double latitude = message.getLocation().getLatitude();
            Double longitude = message.getLocation().getLongitude();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the location pls create a chat connection");
                sendLocation.setChatId(currentChatId.toString());
                sendLocation.setLatitude(latitude);
                sendLocation.setLongitude(longitude);
                execute(sendLocation);

            } else {
                sendLocation.setChatId(adminChatId.toString());
                sendLocation.setLatitude(latitude);
                sendLocation.setLongitude(longitude);
                execute(sendLocation);
                sendMessage(new SendMessage(adminChatId.toString(), sender + ": shared a location"));
            }
        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }
    }

    public void handleContact(Message message, String sender) {
        SendContact sendContact = new SendContact();
        try {
            String phoneNumber = message.getContact().getPhoneNumber();
            String firstName = message.getContact().getFirstName();
            String lastName = message.getContact().getLastName();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the contact pls create the chat connection");
                sendContact.setChatId(currentChatId.toString());
                sendContact.setPhoneNumber(phoneNumber);
                sendContact.setFirstName(firstName);
                if (lastName != null)
                    sendContact.setLastName(lastName);
                execute(sendContact);
            } else {
                sendContact.setChatId(adminChatId.toString());
                sendContact.setPhoneNumber(phoneNumber);
                sendContact.setFirstName(sender + " - " + firstName);
                if (lastName != null)
                    sendContact.setLastName(lastName);
                execute(sendContact);
            }
        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }
    }

    public void handleVenue(Message message, String sender) {
        SendVenue sendVenue = new SendVenue();
        try {
            Double latitude = message.getVenue().getLocation().getLatitude();
            Double longitude = message.getVenue().getLocation().getLongitude();
            String title = message.getVenue().getTitle();
            String address = message.getVenue().getAddress();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the venue pls create a chat connection");
                sendVenue.setChatId(currentChatId.toString());
                sendVenue.setLatitude(latitude);
                sendVenue.setLongitude(longitude);
                sendVenue.setTitle(title);
                sendVenue.setAddress(address);
                execute(sendVenue);

            } else {
                sendVenue.setChatId(adminChatId.toString());
                sendVenue.setLatitude(latitude);
                sendVenue.setLongitude(longitude);
                sendVenue.setTitle(sender + " - " + title);
                sendVenue.setAddress(address);
                execute(sendVenue);
            }
        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }
    }

    public void handlePoll(Message message, String sender) {
        SendPoll sendPoll = new SendPoll();
        try {
            String question = message.getPoll().getQuestion();
            List<String> options = message.getPoll().getOptions().stream()
                    .map(option -> option.getText())
                    .collect(Collectors.toList());
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the poll pls create the chat connection");
                sendPoll.setChatId(currentChatId.toString());
                sendPoll.setQuestion(question);
                sendPoll.setOptions(options);
                execute(sendPoll);

            } else {
                sendPoll.setChatId(adminChatId.toString());
                sendPoll.setQuestion(sender + ": " + question);
                sendPoll.setOptions(options);
                execute(sendPoll);
            }
        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }
    }

    public void handleDice(Message message, String sender) {
        SendDice sendDice = new SendDice();
        try {
            String emoji = message.getDice().getEmoji();
            if (message.getChatId().equals(adminChatId)) {
                if (currentChatId == null)
                    throw new RecordNotFoundException("Failed to deliver the dice pls create the chat connection");
                sendDice.setChatId(currentChatId.toString());
                sendDice.setEmoji(emoji);
                execute(sendDice);

            } else {
                sendDice.setChatId(adminChatId.toString());
                sendDice.setEmoji(emoji);
                execute(sendDice);
                sendMessage(new SendMessage(adminChatId.toString(), sender + ": rolled a dice"));
            }
        } catch (Exception e) {
            log.info("Error-----------:", e);
            throw new RecordNotFoundException("Error!!!!!!!!!");
        }
    }

    private void startBingoGame(Message message) {
        try {
            Long chatId = message.getChatId();
            BingoGame game = bingoService.startNewGame(chatId);

            // Call first number automatically
            game.callNextNumber();

            SendMessage msg = new SendMessage();
            msg.setChatId(chatId.toString());
            msg.setText(bingoService.buildStatusText(game));
            msg.setParseMode("Markdown");
            msg.setReplyMarkup(bingoService.buildBoardKeyboard(game));
            execute(msg);

        } catch (Exception e) {
            log.error("Error starting bingo", e);
        }
    }

    private void handleCallbackQuery2(CallbackQuery query) {
        String data = query.getData();
        Long chatId = query.getMessage().getChatId();
        Integer messageId = query.getMessage().getMessageId();

        try {
            // Bingo game callbacks
            if (data.startsWith("bingo_")) {
                handleBingoCallback(query, data, chatId, messageId);
                return;
            }

            // Answer callback to remove loading spinner
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(query.getId());
            execute(answer);

        } catch (Exception e) {
            log.error("Error handling callback", e);
        }
    }

    private void handleBingoCallback(CallbackQuery query, String data, Long chatId, Integer messageId) {
        try {
            BingoGame game = bingoService.getGame(chatId);
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(query.getId());

            if (game == null || game.isGameOver()) {
                answer.setText("No active game. Send /bingo to start!");
                execute(answer);
                return;
            }

            switch (data) {
                case "bingo_header":
                case "bingo_free":
                case "bingo_marked":
                    answer.setText("Already marked!");
                    execute(answer);
                    return;

                case "bingo_call":
                    int called = game.callNextNumber();
                    if (called == -1) {
                        answer.setText("All numbers have been called!");
                    } else {
                        answer.setText("Called: " + game.getLetterForNumber(called) + "-" + called);
                    }
                    execute(answer);
                    break;

                case "bingo_check":
                    if (game.checkWin()) {
                        answer.setText("🎉🎉🎉 BINGO! YOU WIN! 🎉🎉🎉");
                        game.setGameOver(true);
                        execute(answer);

                        // Send win message
                        SendMessage winMsg = new SendMessage();
                        winMsg.setChatId(chatId.toString());
                        winMsg.setText("🎉🎉🎉\n\n*BINGO! YOU WIN!*\n\n" +
                                "Numbers called: " + game.getCalledNumbers().size() + "\n\n" +
                                "Send /bingo to play again! 🎰");
                        winMsg.setParseMode("Markdown");
                        execute(winMsg);
                        bingoService.endGame(chatId);
                        return;
                    } else {
                        answer.setText("❌ Not BINGO yet! Keep playing!");
                        execute(answer);
                    }
                    break;

                case "bingo_quit":
                    answer.setText("Game ended!");
                    execute(answer);
                    bingoService.endGame(chatId);

                    SendMessage quitMsg = new SendMessage();
                    quitMsg.setChatId(chatId.toString());
                    quitMsg.setText("Game ended. Send /bingo to start a new game! 🎰");
                    execute(quitMsg);
                    return;

                default:
                    break;
            }

            // Handle marking a number
            if (data.startsWith("bingo_mark_")) {
                String[] parts = data.split("_");
                int row = Integer.parseInt(parts[2]);
                int col = Integer.parseInt(parts[3]);

                if (game.markNumber(row, col)) {
                    answer.setText("✅ Marked!");
                    execute(answer);

                    // Auto-check win after marking
                    if (game.checkWin()) {
                        SendMessage winMsg = new SendMessage();
                        winMsg.setChatId(chatId.toString());
                        winMsg.setText("🎉🎉🎉\n\n*BINGO! YOU WIN!*\n\n" +
                                "Numbers called: " + game.getCalledNumbers().size() + "\n\n" +
                                "Send /bingo to play again! 🎰");
                        winMsg.setParseMode("Markdown");
                        execute(winMsg);
                        bingoService.endGame(chatId);
                        return;
                    }
                } else {
                    answer.setText("❌ This number hasn't been called yet!");
                    execute(answer);
                    return;
                }
            }

            if (data.startsWith("bingo_notcalled_")) {
                answer.setText("❌ This number hasn't been called yet!");
                execute(answer);
                return;
            }

            // Update the board
            EditMessageText editMsg = new EditMessageText();
            editMsg.setChatId(chatId.toString());
            editMsg.setMessageId(messageId);
            editMsg.setText(bingoService.buildStatusText(game));
            editMsg.setParseMode("Markdown");
            editMsg.setReplyMarkup(bingoService.buildBoardKeyboard(game));
            execute(editMsg);

        } catch (Exception e) {
            log.error("Error in bingo callback", e);
        }
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery());
            return;
        }
        if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();

            // Check for pending bingo input first
            if (message.hasText() && bingoHandler.hasPendingAction(chatId)) {
                try {
                    SendMessage response = bingoHandler.handleTextInput(chatId, message.getText());
                    if (response != null) {
                        execute(response);
                        return;
                    }
                } catch (Exception e) {
                    log.error("Error handling bingo input", e);
                }
            }
            String activeHostId = bingoHandler.getPlayerGameId(chatId);
            GameSession activeSession = null;

            if (activeHostId != null) {
                activeSession = bingoGameService.getGame(activeHostId);
                if (activeSession == null || activeSession.getStatus() == BingoEnums.GameStatus.FINISHED) {
                    activeSession = null;
                }
            }

            // Handle /bingo command
            if (activeSession == null && message.hasText() && message.getText().equals("/bingo")) {
                try {
                    execute(bingoHandler.showWelcomeMenu(chatId));
                } catch (Exception e) {
                    log.error("Error", e);
                }
                return;
            }

            if (message.hasText()) {
                try {

                    // Only show hint if NOT in an active game
                    if (activeSession == null) {
                        SendMessage msg = new SendMessage();
                        msg.setChatId(chatId.toString());
                        msg.setText("🎮 Send /bingo to start a game!");
                        execute(msg);
                    }
                } catch (Exception e) {
                    log.error("Error", e);
                }
            }

        }
    }

    private void handleCallbackQuery(CallbackQuery query) {
        String data = query.getData();
        Long chatId = query.getMessage().getChatId();
        Integer messageId = query.getMessage().getMessageId();

        try {
            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(query.getId());

            // ===== NON-GAME CALLBACKS (always allowed) =====
            if (data.equals("bingo_noop")) {
                answer.setText("Already marked!");
                execute(answer);
                return;
            }
            if (data.equals("bingo_play")) {
                execute(answer);
                execute(bingoHandler.showPlayMenu(chatId));
                return;
            }
            if (data.equals("bingo_back")) {
                execute(answer);
                execute(bingoHandler.showWelcomeMenu(chatId));
                return;
            }
            if (data.equals("bingo_create")) {
                execute(answer);
                execute(bingoHandler.handleCreateHost(chatId));
                return;
            }
            if (data.equals("bingo_join")) {
                execute(answer);
                execute(bingoHandler.handleJoinParty(chatId));
                return;
            }

            // ===== GAME CALLBACKS (need validation) =====

            // Extract hostId from callback data
            String hostId = extractHostId(data);

            if (hostId != null && isGameExpired(hostId, chatId, answer)) {
                return; // Already sent "session expired" message
            }

            if (data.startsWith("bingo_addbot_")) {
                hostId = data.replace("bingo_addbot_", "");
                String result = bingoGameService.addBots(hostId);
                if ("FULL".equals(result)) {
                    answer.setText("❌ Party is full! (Max 6)");
                } else {
                    answer.setText("🤖 Bot added!");
                }
                execute(answer);
                GameSession session = bingoGameService.getGame(hostId);
                editLobbyMessage(chatId, messageId, session);
            } else if (data.startsWith("bingo_refresh_")) {
                hostId = data.replace("bingo_refresh_", "");
                execute(answer);
                GameSession session = bingoGameService.getGame(hostId);
                editLobbyMessage(chatId, messageId, session);
            } else if (data.startsWith("bingo_startmenu_")) {
                hostId = data.replace("bingo_startmenu_", "");
                execute(answer);
                execute(bingoHandler.showBoardSizeMenu(chatId, hostId));
            } else if (data.startsWith("bingo_size_")) {
                String[] parts = data.split("_");
                int size = Integer.parseInt(parts[2]);
                hostId = parts[3];
                execute(answer);
                startBingoGame(hostId, size);
            } else if (data.startsWith("bingo_select_")) {
                String[] parts = data.split("_");
                int number = Integer.parseInt(parts[2]);
                hostId = parts[3];
                handleBingoMove(chatId, hostId, number, messageId, answer);
            } else if (data.startsWith("bingo_quit_")) {
                hostId = data.replace("bingo_quit_", "");
                answer.setText("Game ended!");
                execute(answer);
                bingoGameService.stopGame(hostId);
                GameSession session = bingoGameService.getGame(hostId);
                sendToAllPlayersEdit(hostId, "🛑 *Game stopped!*\n\nSend /bingo to play again! 🎮");
                bingoHandler.clearAllPlayerData(session);
                bingoGameService.cleanupGame(hostId);
            } else if (data.startsWith("bingo_cancel_")) {
                hostId = data.replace("bingo_cancel_", "");
                answer.setText("Game cancelled!");
                execute(answer);
                GameSession session = bingoGameService.getGame(hostId);
                if (session != null) {
                    bingoHandler.clearAllPlayerData(session);
                    bingoGameService.cleanupGame(hostId);
                }
            } else {
                execute(answer);
            }

        } catch (Exception e) {
            log.error("Error handling callback", e);
        }
    }

    private String extractHostId(String callbackData) {
        try {
            if (callbackData.startsWith("bingo_addbot_")) return callbackData.replace("bingo_addbot_", "");
            if (callbackData.startsWith("bingo_refresh_")) return callbackData.replace("bingo_refresh_", "");
            if (callbackData.startsWith("bingo_startmenu_")) return callbackData.replace("bingo_startmenu_", "");
            if (callbackData.startsWith("bingo_cancel_")) return callbackData.replace("bingo_cancel_", "");
            if (callbackData.startsWith("bingo_quit_")) return callbackData.replace("bingo_quit_", "");
            if (callbackData.startsWith("bingo_size_")) {
                String[] parts = callbackData.split("_");
                return parts[3];
            }
            if (callbackData.startsWith("bingo_select_")) {
                String[] parts = callbackData.split("_");
                return parts[3];
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private void sendToAllPlayersEdit(String hostId, String text) {
        GameSession session = bingoGameService.getGame(hostId);
        if (session == null) return;

        for (Player player : session.getPlayers()) {
            if (player.getIsBot()) continue;
            Integer msgId = bingoHandler.getPlayerBoardMsgId(player.getChatId());
            try {
                if (msgId != null) {
                    EditMessageText edit = new EditMessageText();
                    edit.setChatId(player.getChatId().toString());
                    edit.setMessageId(msgId);
                    edit.setText(text);
                    edit.setParseMode("Markdown");
                    execute(edit);
                } else {
                    SendMessage msg = new SendMessage();
                    msg.setChatId(player.getChatId().toString());
                    msg.setText(text);
                    msg.setParseMode("Markdown");
                    execute(msg);
                }
            } catch (Exception e) {
                log.error("Error sending to: {}", player.getPlayerName(), e);
            }
        }
    }

    private void editLobbyMessage(Long chatId, Integer messageId, GameSession session) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("👑 *GAME LOBBY*\n\n");
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            sb.append("🔑 Host ID: `").append(session.getHostId()).append("`\n");
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n");
            sb.append("👥 *Players* (").append(session.getPlayers().size()).append("/6):\n");

            int i = 1;
            for (Player p : session.getPlayers()) {
                String icon = p.getIsHost() ? "👑" : p.getIsBot() ? "🤖" : "👤";
                sb.append(icon).append(" ").append(i++).append(". ").append(p.getPlayerName()).append("\n");
            }

            EditMessageText edit = new EditMessageText();
            edit.setChatId(chatId.toString());
            edit.setMessageId(messageId);
            edit.setText(sb.toString());
            edit.setParseMode("Markdown");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(List.of(createButton("🤖 Play with Bots", "bingo_addbot_" + session.getHostId())));
            keyboard.add(List.of(createButton("🔄 Refresh", "bingo_refresh_" + session.getHostId())));
            keyboard.add(List.of(createButton("🚀 Start Game", "bingo_startmenu_" + session.getHostId())));
            keyboard.add(List.of(createButton("❌ Cancel", "bingo_cancel_" + session.getHostId())));
            markup.setKeyboard(keyboard);
            edit.setReplyMarkup(markup);

            execute(edit);
        } catch (Exception e) {
            if (!e.getMessage().contains("message is not modified")) {
                log.error("Error editing lobby", e);
            }
//            log.error("Error editing lobby", e);
        }
    }

    private void startBingoGame(String hostId, int size) {
        try {
            GameSession session = bingoGameService.startGame(hostId, size);

            bingoTurnService.setOnAutoMoveCallback((hId, result) -> {
                try {
                    handleAutoMove(hId, result);
                } catch (Exception e) {
                    log.error("Error in auto move", e);
                }
            });

            // Clear old message IDs
            for (Player player : session.getPlayers()) {
                if (!player.getIsBot()) {
                    bingoHandler.clearPlayerBoardMsgId(player.getChatId());
                }
            }

            // Send initial board to all human players
            for (Player player : session.getPlayers()) {
                if (player.getIsBot()) continue;
                sendBoardToPlayer(session, player);
            }

            bingoTurnService.startTurnTimer(hostId);

            // Handle bot's first turn
            Player currentPlayer = session.getCurrentTurnPlayer();
            if (currentPlayer != null && currentPlayer.getIsBot()) {
                handleBotTurn(hostId);
            }

        } catch (Exception e) {
            log.error("Error starting game", e);
        }
    }

    private void handleBingoMove(Long chatId, String hostId, int number, Integer messageId, AnswerCallbackQuery answer) {
        try {
            Map<String, Object> result = bingoGameService.makeMove(hostId, chatId, number);
            String status = (String) result.get("status");

            switch (status) {
                case "NOT_YOUR_TURN":
                    answer.setText("⏳ Not your turn!");
                    execute(answer);
                    return;
                case "ALREADY_SELECTED":
                    answer.setText("⚠️ Already selected!");
                    execute(answer);
                    return;
                case "NOT_PLAYING":
                    answer.setText("Game not active!");
                    execute(answer);
                    return;
                default:
                    answer.setText("✅ " + number);
                    execute(answer);
            }

            bingoTurnService.cancelTurnTimer(hostId);
            GameSession session = bingoGameService.getGame(hostId);

            if (Boolean.TRUE.equals(result.get("gameOver"))) {
                handleGameOver(hostId, session);
            } else {
                // Update all human players' boards
                for (Player player : session.getPlayers()) {
                    if (player.getIsBot()) continue;
                    sendBoardToPlayer(session, player);
                }

                bingoTurnService.startTurnTimer(hostId);

                Player nextPlayer = session.getCurrentTurnPlayer();
                if (nextPlayer != null && nextPlayer.getIsBot()) {
                    handleBotTurn(hostId);
                }
            }

        } catch (Exception e) {
            log.error("Error handling move", e);
        }
    }

    private void handleBotTurn(String hostId) {
        new Thread(() -> {
            try {
                Thread.sleep(2000);
                bingoTurnService.cancelTurnTimer(hostId);

                Map<String, Object> result = bingoGameService.botMove(hostId);
                GameSession session = bingoGameService.getGame(hostId);

                if (Boolean.TRUE.equals(result.get("gameOver"))) {
                    handleGameOver(hostId, session);
                } else {
                    for (Player player : session.getPlayers()) {
                        if (player.getIsBot()) continue;
                        sendBoardToPlayer(session, player);
                    }

                    bingoTurnService.startTurnTimer(hostId);

                    Player next = session.getCurrentTurnPlayer();
                    if (next != null && next.getIsBot()) {
                        handleBotTurn(hostId);
                    }
                }
            } catch (Exception e) {
                log.error("Error in bot turn", e);
            }
        }).start();
    }

    private void handleAutoMove(String hostId, Map<String, Object> result) {
        try {
            GameSession session = bingoGameService.getGame(hostId);

            if (Boolean.TRUE.equals(result.get("gameOver"))) {
                handleGameOver(hostId, session);
            } else {
                // Update all boards (timeout info is in the game UI via last move)
                for (Player player : session.getPlayers()) {
                    if (player.getIsBot()) continue;
                    sendBoardToPlayer(session, player);
                }

                Player next = session.getCurrentTurnPlayer();
                if (next != null && next.getIsBot()) {
                    handleBotTurn(hostId);
                }
            }
        } catch (Exception e) {
            log.error("Error handling auto move", e);
        }
    }

    private void sendBoardToPlayer(GameSession session, Player player) {
        try {
            String ui = bingoHandler.buildGameUI(session, player);
            InlineKeyboardMarkup keyboard = bingoHandler.buildGameKeyboard(session, player);

            Integer existingMsgId = bingoHandler.getPlayerBoardMsgId(player.getChatId());

            if (existingMsgId != null) {
                // EDIT existing message
                try {
                    EditMessageText edit = new EditMessageText();
                    edit.setChatId(player.getChatId().toString());
                    edit.setMessageId(existingMsgId);
                    edit.setText(ui);
                    edit.setParseMode("Markdown");
                    edit.setReplyMarkup(keyboard);
                    execute(edit);
                    return;
                } catch (Exception e) {
                    log.warn("Edit failed, sending new: {}", e.getMessage());
                }
            }

            // Send NEW message (first time only)
            SendMessage msg = new SendMessage();
            msg.setChatId(player.getChatId().toString());
            msg.setText(ui);
            msg.setParseMode("Markdown");
            msg.setReplyMarkup(keyboard);
            Message sent = execute(msg);

            // Store message ID
            bingoHandler.setPlayerBoardMsgId(player.getChatId(), sent.getMessageId());

        } catch (Exception e) {
            log.error("Error sending board to: {}", player.getPlayerName(), e);
        }
    }

    private void sendToAllPlayers(String hostId, String message) {
        GameSession session = bingoGameService.getGame(hostId);
        if (session == null) return;

        for (Player player : session.getPlayers()) {
            if (player.getIsBot()) continue;
            try {
                SendMessage msg = new SendMessage();
                msg.setChatId(player.getChatId().toString());
                msg.setText(message);
                msg.setParseMode("Markdown");
                execute(msg);
            } catch (Exception e) {
                log.error("Error sending to player: {}", player.getPlayerName(), e);
            }
        }
    }

    private void refreshLobby(Long chatId, String hostId, Integer messageId) {
        try {
            GameSession session = bingoGameService.getGame(hostId);
            StringBuilder sb = new StringBuilder();
            sb.append("👑 *Game Lobby*\n\n");
            sb.append("🔑 Host ID: *").append(hostId).append("*\n\n");
            sb.append("👥 Players (").append(session.getPlayers().size()).append("/6):\n");
            int i = 1;
            for (Player p : session.getPlayers()) {
                String role = p.getIsHost() ? " (Host)" : "";
                String bot = p.getIsBot() ? " 🤖" : "";
                sb.append(i++).append(". ").append(p.getPlayerName()).append(role).append(bot).append("\n");
            }

            EditMessageText edit = new EditMessageText();
            edit.setChatId(chatId.toString());
            edit.setMessageId(messageId);
            edit.setText(sb.toString());
            edit.setParseMode("Markdown");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
            keyboard.add(List.of(createButton("🤖 Play with Bot", "bingo_addbot_" + hostId)));
            keyboard.add(List.of(createButton("🔄 Refresh Players", "bingo_refresh_" + hostId)));
            keyboard.add(List.of(createButton("🚀 Start Game", "bingo_startmenu_" + hostId)));
            keyboard.add(List.of(createButton("❌ Cancel", "bingo_cancel_" + hostId)));
            markup.setKeyboard(keyboard);
            edit.setReplyMarkup(markup);

            execute(edit);
        } catch (Exception e) {
            log.error("Error refreshing lobby", e);
        }
    }

    private InlineKeyboardButton createButton(String text, String data) {
        InlineKeyboardButton btn = new InlineKeyboardButton();
        btn.setText(text);
        btn.setCallbackData(data);
        return btn;
    }

    private void handleGameOver(String hostId, GameSession session) {
        try {
            bingoTurnService.cancelTurnTimer(hostId);

            String gameOverUI = bingoHandler.buildGameOverUI(session);

            // Edit all players' board messages with final result
            for (Player player : session.getPlayers()) {
                if (player.getIsBot()) continue;
                Integer msgId = bingoHandler.getPlayerBoardMsgId(player.getChatId());
                if (msgId != null) {
                    try {
                        EditMessageText edit = new EditMessageText();
                        edit.setChatId(player.getChatId().toString());
                        edit.setMessageId(msgId);
                        edit.setText(gameOverUI);
                        edit.setParseMode("Markdown");
                        execute(edit);
                    } catch (Exception e) {
                        // If edit fails, send new
                        SendMessage msg = new SendMessage();
                        msg.setChatId(player.getChatId().toString());
                        msg.setText(gameOverUI);
                        msg.setParseMode("Markdown");
                        execute(msg);
                    }
                }
            }

            // Cleanup
            bingoHandler.clearAllPlayerData(session);
            bingoGameService.cleanupGame(hostId);

        } catch (Exception e) {
            log.error("Error handling game over", e);
        }
    }

    private boolean isGameExpired(String hostId, Long chatId, AnswerCallbackQuery answer) {
        try {
            if (hostId == null || hostId.isEmpty()) {
                answer.setText("⚠️ Session expired! Send /bingo to start.");
                execute(answer);
                return true;
            }

            GameSession session = bingoGameService.getGame(hostId);

            if (session == null || session.getStatus() == BingoEnums.GameStatus.FINISHED) {
                answer.setText("⚠️ Session expired! Send /bingo to start.");
                execute(answer);
                return true;
            }

            return false;
        } catch (Exception e) {
            return true;
        }
    }

}
