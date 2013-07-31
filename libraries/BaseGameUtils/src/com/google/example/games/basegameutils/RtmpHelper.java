package com.google.example.games.basegameutils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;

import java.util.ArrayList;
import java.util.List;






// WARNING: as of this date (2013-07-30) this feature is EXPERIMENTAL and
// largely undocumented. Bug fixes and documentation coming soon.   -- Bruno Oliveira.








/**
 * Multiplayer game helper.
 */
public class RtmpHelper implements RoomUpdateListener, RoomStatusUpdateListener,
        RealTimeMessageReceivedListener, OnInvitationReceivedListener {
    protected GameHelper mGameHelper;

    public interface RtmpListener {
        public void onRtmpPreparing(); // preparing to start a game (show wait screen)
        public void onRtmpStarted(); // started a game
        public void onRtmpEnding(int reason); // ending a game (show wait screen)
        public void onRtmpEnded(int reason); // ended a game
        public void onRtmpAdded(Participant participant);
        public void onRtmpDropped(Participant participant);
        public void onRtmpMessage(RealTimeMessage msg);
        public void onRtmpInvitation(Invitation inv);
    }

    // our listener
    RtmpListener mRtmpListener = null;

    // if true, we should start the game as soon as we get the minimum # of players
    // if false, we should wait until everyone is connected before starting.
    boolean mStartEarly = false;

    // minimum/maximum # of players, INCLUDING me
    int mMinPlayers = 0;
    int mMaxPlayers = 0;

    // what Activity result are we expecting?
    final static int EXPECTING_NOTHING = 0;
    final static int EXPECTING_PLAYERS = 1;
    final static int EXPECTING_INBOX = 2;
    final static int EXPECTING_WAITING_ROOM = 3;
    int mExpectedActivityResult = EXPECTING_NOTHING;

    // state of multiplayer:
    final static int MP_NOT_SET_UP = 0;
    final static int MP_SETTING_UP = 1;
    final static int MP_WAITING = 2; // connected to room, waiting for players
    final static int MP_PLAYING = 3; // playing the game
    final static int MP_LEAVING = 4;
    final static String[] MP_STATE_NAMES = {
            "MP_NOT_SET_UP", "MP_SETTING_UP", "MP_WAITING", "MP_PLAYING", "MP_LEAVING"
    };
    int mMpState = MP_NOT_SET_UP;

    // Flag indicating whether or not waiting room was dismissed from code
    // (if so, we should ignore its result)
    boolean mWaitRoomDismissedFromCode = false;

    // The room we are in, if any
    Room mRoom = null;

    // What is my ID?
    String mMyId = null;

    // List of connected participants
    List<Participant> mConnectedParticipants = new ArrayList<Participant>();
    List<Participant> mConnectedParticipantsExcludingSelf = new ArrayList<Participant>();

    // the reason why we are ending a game
    public final static int END_REASON_NORMAL = 0; // game wanted to end
    public final static int END_REASON_USER_CANCELLED = 1; // user cancelled
    public final static int END_REASON_APP_STOPPED = 2;  // we got onStop
    public final static int END_REASON_INSUFFICIENT_PLAYERS = 3; // not enough players to play
    public final static int END_REASON_ERROR = 4; // an error occurred
    int mGameEndReason = END_REASON_NORMAL;

    // Temp buffers for message sending, sorted from MOST RECENTLY used to LEAST RECENTLY used
    ArrayList<byte[]> mTempBuffers = new ArrayList<byte[]>();
    final static int MAX_TEMP_BUFFERS = 64;  // max # of temp buffers to keep in memory

    // Special message that we send to peers to notify that the game is starting
    // (so they know to dismiss the waiting room)

    public RtmpHelper(GameHelper gameHelper, RtmpListener listener) {
        mGameHelper = gameHelper;
        mRtmpListener = listener;
    }

    /**
     * Whether to allow the game to start before all participants have joined. If set to true,
     * then the game can start if the minimum number of participants is reached. If set to
     * false (default), all participants must be in the room in order for the game to start.
     */
    public void enableEarlyStart(boolean enable) {
        assertState("enableEarlyStart", MP_NOT_SET_UP);
        mStartEarly = true;
    }

    public void startWithInviteDialog(int minOpponents, int maxOpponents) {
        assertHasActivityAndListener("startWithInviteDialog");
        assertState("startWithInviteDialog", MP_NOT_SET_UP);
        debugLog("Starting game with invite dialog.");
        mMinPlayers = minOpponents + 1;
        mMaxPlayers = maxOpponents + 1;
        Intent i = mGameHelper.getGamesClient().getSelectPlayersIntent(minOpponents, maxOpponents);
        debugLog("Starting select-players UI, min=" + minOpponents + ", max=" + maxOpponents);
        setState(MP_SETTING_UP);
        mExpectedActivityResult = EXPECTING_PLAYERS;
        mGameHelper.mActivity.startActivityForResult(i, GameHelper.RC_MULTIPLAYER);
    }

    public void startWithRandomOpponents(int minOpponents, int maxOpponents) {
        assertHasActivityAndListener("startWithRandomOpponents");
        assertState("startWithRandomOpponents", MP_NOT_SET_UP);
        debugLog("Starting game with random opponents.");
        mMinPlayers = 1 + minOpponents;
        mMaxPlayers = 1 + maxOpponents;
        debugLog("Creating automatch room, opponents: " + minOpponents + "-" + maxOpponents);
        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minOpponents, maxOpponents, 0);
        RoomConfig.Builder rtmConfigBuilder = makeBasicConfigBuilder();
        rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        setState(MP_SETTING_UP);
        mGameHelper.getGamesClient().createRoom(rtmConfigBuilder.build());
        debugLog("Automatch room created, waiting for it to be ready...");
    }

    public void startWithSpecificPlayers(List<String> idsToInvite, int minPlayers) {
        assertHasActivityAndListener("startWithSpecificPlayers");
        assertState("startWithSpecificPlayers", MP_NOT_SET_UP);
        debugLog("Starting game with specific opponents.");
        debugLog("List has " + idsToInvite.size() + " players, minimum is + " + minPlayers);
        mMinPlayers = minPlayers;
        mMaxPlayers = idsToInvite.size();
        RoomConfig.Builder rtmConfigBuilder = makeBasicConfigBuilder();
        ArrayList<String> toInvite = new ArrayList<String>();
        toInvite.addAll(idsToInvite);
        rtmConfigBuilder.addPlayersToInvite(toInvite);
        setState(MP_SETTING_UP);
        mGameHelper.getGamesClient().createRoom(rtmConfigBuilder.build());
        debugLog("Room created, waiting for it to be ready...");
    }

    public void startWithInvitationInbox() {
        assertHasActivityAndListener("startWithInvitationInbox");
        assertState("startWithInvitationInbox", MP_NOT_SET_UP);
        debugLog("Starting game with invitation inbox.");
        mExpectedActivityResult = EXPECTING_INBOX;
        setState(MP_SETTING_UP);
        Intent i = mGameHelper.getGamesClient().getInvitationInboxIntent();
        mGameHelper.mActivity.startActivityForResult(i, GameHelper.RC_MULTIPLAYER);
    }

    public void startWithInvitation(Invitation inv) {
        assertHasActivityAndListener("startWithInvitation");
        assertState("startWithInvitation", MP_NOT_SET_UP);
        if (inv == null) {
            throw new IllegalArgumentException("inv is NULL in startWithInviation");
        }
        debugLog("Starting game with invitation: " + inv.getInvitationId());
        acceptInviteToRoom(inv.getInvitationId());
    }

    RoomConfig.Builder makeBasicConfigBuilder() {
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        return rtmConfigBuilder;
    }

    public void end() {
        debugLog("Requested end of multiplayer game (normal).");
        shutdownGame(END_REASON_NORMAL);
    }

    public List<Participant> getConnectedParticipants() {
        return getConnectedParticipants(true);
    }

    public List<Participant> getConnectedParticipants(boolean includeSelf) {
        assertState("getConnectedParticipants", MP_WAITING, MP_PLAYING);
        return includeSelf ? mConnectedParticipants : mConnectedParticipantsExcludingSelf;
    }

    public void broadcast(boolean reliable, byte[] buf) {
        send(null, reliable, buf, 0, buf.length);
    }
    public void broadcast(boolean reliable, byte[] buf, int offset, int length) {
        send(null, reliable, buf, offset, length);
    }
    public void send(String to, boolean reliable, byte[] buf) {
        send(to, reliable, buf, 0, buf.length);
    }
    public void send(String to, boolean reliable, byte[] buf, int offset, int length) {
        assertState("send", MP_PLAYING, MP_WAITING);
        GamesClient gc = mGameHelper.getGamesClient();
        byte[] bufToSend = buf;

        if (offset != 0 || length != buf.length) {
            // TODO: update this when we get API support for sending a part of the buffer
            // For now, we have to work around this by allocating a buffer of the appropriate
            // size and copying the message to that buffer.
            bufToSend = getTempBuffer(length);
            System.arraycopy(buf, offset, bufToSend, 0, length);
        }

        if (to == null) {
            // broadcast to all connected participants except myself
            for (Participant p : mConnectedParticipantsExcludingSelf) {
                send(p.getParticipantId(), reliable, buf, offset, length);
            }
        } else if (reliable) {
            // send reliable message to given recipient
            gc.sendReliableRealTimeMessage(null, bufToSend, mRoom.getRoomId(), to);
        } else {
            // send unreliable message to given recipient
            gc.sendUnreliableRealTimeMessage(bufToSend, mRoom.getRoomId(), to);
        }
    }

    public boolean isPlaying() {
        return mMpState == MP_PLAYING;
    }

    public boolean amIAlone() {
        if (mMpState == MP_PLAYING || mMpState == MP_WAITING) {
            return mConnectedParticipantsExcludingSelf.size() == 0;
        } else {
            return true;
        }
    }

    public void forceStart() {
        debugLog("Forcing start of game.");
        if (mMpState == MP_WAITING) {
            dismissWaitingRoom();
            startGame();
        } else {
            logWarn("*** RtmpHelper.forceStart called while not in MP_WAITING state, " +
                    "state was " + MP_STATE_NAMES[mMpState]);
        }
    }


    byte[] getTempBuffer(int size) {
        int i;
        for (i = 0; i < mTempBuffers.size(); ++i) {
            if (mTempBuffers.get(i).length == size) {
                // found a suitable buffer to reuse
                byte[] buf = mTempBuffers.get(i);
                if (i != 0) {
                    // move it to the first element of the array, as it's the most recently used
                    mTempBuffers.remove(i);
                    mTempBuffers.add(0, buf);
                }
                return buf;
            }
        }
        // no buffer to reuse, so create a new one
        byte[] buf = new byte[size];
        mTempBuffers.add(0, buf);
        while (mTempBuffers.size() > MAX_TEMP_BUFFERS) {
            // discard least recently used buffer
            mTempBuffers.remove(mTempBuffers.size() - 1);
        }
        return buf;
    }


    void assertState(String operation, int... expectedStates) {
        for (int expectedState : expectedStates) {
            if (mMpState == expectedState) {
                return;
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("RtmpHelper: operation attempted at incorrect state. ");
        sb.append("Operation: ").append(operation).append(". ");
        sb.append("State: ").append(MP_STATE_NAMES[mMpState]).append(". ");
        if (expectedStates.length == 1) {
            sb.append("Expected state: ").append(MP_STATE_NAMES[expectedStates[0]]).append(".");
        } else {
            sb.append("Expected states:");
            for (int expectedState : expectedStates) {
                sb.append(" " ).append(MP_STATE_NAMES[expectedState]);
            }
            sb.append(".");
        }
        logError(sb.toString());
        throw new IllegalStateException(sb.toString());
    }

    void setState(int newState) {
        if (newState == mMpState) {
            // nothing to do
            return;
        }
        String oldStateName = MP_STATE_NAMES[mMpState];
        String newStateName = MP_STATE_NAMES[newState];
        mMpState = newState;
        debugLog("Multiplayer state change " + oldStateName + " -> " + newStateName);

        Activity a = mGameHelper.mActivity;
        if (a != null) {
            if (mMpState != MP_NOT_SET_UP) {
                debugLog("FLAG_KEEP_SCREEN_ON: enabled.");
                a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                debugLog("FLAG_KEEP_SCREEN_ON: disabled");
                a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } else {
            debugLog("Warning: can't adjust FLAG_KEEP_SCREEN_ON because Activity is null");
        }

        // Call the appropriate callback for the new state
        if (mRtmpListener != null) {
            switch (newState) {
                case MP_NOT_SET_UP:
                    debugLog("Calling callback: onRtmpEnded");
                    mRtmpListener.onRtmpEnded(mGameEndReason);
                    break;
                case MP_LEAVING:
                    debugLog("Calling callback: onRtmpEnding, " +
                            shutdownReasonToString(mGameEndReason));
                    mRtmpListener.onRtmpEnding(mGameEndReason);
                    break;
                case MP_SETTING_UP:
                    debugLog("Calling callback: onRtmpPreparing");
                    mRtmpListener.onRtmpPreparing();
                    break;
                case MP_PLAYING:
                    debugLog("Calling callback: onRtmpStarted");
                    mRtmpListener.onRtmpStarted();
                    break;
                case MP_WAITING:
                    // nothing
                    break;
            }
        }
    }

    void assertHasActivityAndListener(String method) {
        if (mRtmpListener == null) {
            throw new IllegalStateException("To use RtmpHelper multiplayer features," +
                    " you must install a RealTimeMpHelperListener first. " +
                    " (method: " + method + ")");
        }
        Activity act = mGameHelper.mActivity;
        if (act == null) {
            throw new IllegalStateException("RtmpHelper." + method + " can only be " +
                    "called when Activity is onscreen.");
        }
    }

    void onActivityResult(int response, Intent data) {
        if (mExpectedActivityResult == EXPECTING_NOTHING) {
            debugLog("Warning: RtmpHelper got an unexpected Activity result.");
            return;
        }

        int expected = mExpectedActivityResult;
        mExpectedActivityResult = EXPECTING_NOTHING;

        if (expected == EXPECTING_PLAYERS) {
            handleSelectPlayersResult(response, data);
        } else if (expected == EXPECTING_INBOX) {
            handleInvitationInboxResult(response, data);
        } else if (expected == EXPECTING_WAITING_ROOM) {
            handleWaitingRoomResult(response, data);
        } else {
            throw new RuntimeException("Unexpected Activity Result. Expected: " + expected);
        }
    }

    void onGamesClientConnected(Bundle hint) {
        // register invitation listener
        mGameHelper.getGamesClient().registerInvitationListener(this);

        // check connection hint
        if (hint != null) {
            Invitation inv = hint.getParcelable(GamesClient.EXTRA_INVITATION);
            debugLog("Got invitation from connection hint. Accepting.");
            if (inv == null || inv.getInvitationId() == null) {
                debugLog("Error: invitation was NULL!");
                return;
            } else {
                acceptInviteToRoom(inv.getInvitationId());
            }
        }
    }


    void onStop() {
        // if we are in a room, leave it
        if (mRoom != null) {
            debugLog("RtmpHelper got onStop, cleaning up room, if any.");
            shutdownGame(END_REASON_APP_STOPPED);
        }
    }

    void handleSelectPlayersResult(int response, Intent data) {
        debugLog("Got select players UI response.");
        if (response != Activity.RESULT_OK) {
            debugLog("Select players UI cancelled, " + response);
            shutdownGame(END_REASON_USER_CANCELLED);
            return;
        }
        if (mMpState != MP_SETTING_UP) {
            // we should not be getting this now
            logError("Error: result from select players dialog arrived unexpectedly.");
            return;
        }

        debugLog("Select players UI succeeded.");

        // get the invitee list
        final ArrayList<String> invitees = data.getStringArrayListExtra(GamesClient.EXTRA_PLAYERS);
        debugLog("Invitee count: " + invitees.size());

        // get the automatch criteria
        Bundle autoMatchCriteria = null;
        int minAutoMatchPlayers = data.getIntExtra(GamesClient.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        int maxAutoMatchPlayers = data.getIntExtra(GamesClient.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
        if (minAutoMatchPlayers > 0 || maxAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    minAutoMatchPlayers, maxAutoMatchPlayers, 0);
            debugLog("Automatch criteria: " + autoMatchCriteria);
        }

        // adjust mMaxPlayers to be the maximum possible participants in this game
        mMaxPlayers = invitees.size() + maxAutoMatchPlayers;
        debugLog("Adjusted max # of participants: " + mMaxPlayers);

        // create the room
        debugLog("Creating room...");
        RoomConfig.Builder rtmConfigBuilder = makeBasicConfigBuilder();
        rtmConfigBuilder.addPlayersToInvite(invitees);
        if (autoMatchCriteria != null) {
            rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
        }
        mGameHelper.getGamesClient().createRoom(rtmConfigBuilder.build());
        debugLog("Room created, waiting for it to be ready...");
    }

    void startGame() {
        debugLog("Starting gameplay!");
        setState(MP_PLAYING);
    }

    void endGame() {
        debugLog("End of game, reason " + shutdownReasonToString(mGameEndReason));
        mConnectedParticipants.clear();
        mMaxPlayers = mMinPlayers = 0;
        mMyId = null;
        mRoom = null;
        setState(MP_NOT_SET_UP);
    }

    void handleInvitationInboxResult(int response, Intent data) {
        debugLog("Got invitation inbox UI response.");
        if (response != Activity.RESULT_OK) {
            debugLog("Invitation inbox UI cancelled, " + response);
            shutdownGame(END_REASON_USER_CANCELLED);
            return;
        }
        if (mMpState != MP_SETTING_UP) {
            // we should not be getting this now
            logError("Error: result from invitation inbox arrived unexpectedly.");
            return;
        }

        debugLog("Invitation inbox UI succeeded.");
        Invitation inv = data.getExtras().getParcelable(GamesClient.EXTRA_INVITATION);
        if (inv == null || inv.getInvitationId() == null) {
            // if this happens, it's a bug
            logError("*** Error: invitation inbox had null invite.");
            shutdownGame(END_REASON_ERROR);
            return;
        }
        debugLog("Accepting invitation: " + inv.getInvitationId());

        // accept invitation
        acceptInviteToRoom(inv.getInvitationId());
    }

    void handleWaitingRoomResult(int response, Intent data) {
        if (mWaitRoomDismissedFromCode) {
            mWaitRoomDismissedFromCode = false;
            debugLog("Waiting room dismissed from code (expected). Ignoring.");
            return;
        }

        debugLog("Got waiting room result: " +
                GameHelper.activityResponseCodeToString(response));
        if (mMpState != MP_WAITING) {
            // we should not be getting this now
            logError("Error: result from waiting room arrived unexpectedly.");
            return;
        }

        switch (response) {
            case Activity.RESULT_OK:
                // game should start
                debugLog("Waiting room returned RESULT_OK, starting game!");
                startGame();
                break;
            case Activity.RESULT_CANCELED:
            case GamesActivityResultCodes.RESULT_LEFT_ROOM:
                // game cancelled
                debugLog("Waiting room cancelled: game aborted.");
                shutdownGame(END_REASON_USER_CANCELLED);
                break;
        }
    }

    // Accept the given invitation.
    void acceptInviteToRoom(String invId) {
        // accept the invitation
        assertState("acceptInviteToRoom", MP_NOT_SET_UP);
        debugLog("Accepting invitation: " + invId);
        RoomConfig.Builder roomConfigBuilder = makeBasicConfigBuilder();
        roomConfigBuilder.setInvitationIdToAccept(invId);
        setState(MP_SETTING_UP);
        mGameHelper.getGamesClient().joinRoom(roomConfigBuilder.build());
    }

    String shutdownReasonToString(int reason) {
        switch (reason) {
            case END_REASON_APP_STOPPED:
                return "END_REASON_APP_STOPPED";
            case END_REASON_ERROR:
                return "END_REASON_ERROR";
            case END_REASON_NORMAL:
                return "END_REASON_NORMAL";
            case END_REASON_USER_CANCELLED:
                return "END_REASON_USER_CANCELLED";
            case END_REASON_INSUFFICIENT_PLAYERS:
                return "END_REASON_INSUFFICIENT_PLAYERS";
            default:
                return "Unknown game end reason: " + reason;
        }
    }

    void shutdownGame(int reason) {
        mGameEndReason = reason;
        dismissWaitingRoom();

        if (mMpState == MP_NOT_SET_UP) {
            debugLog("No need to shut down game, no game is currently happening.");
            return;
        }

        debugLog("Shutting down room, reason " + shutdownReasonToString(reason));
        if (mRoom == null) {
            debugLog("Room was null, no cleanup necessary.");
            // so we can go straight to endGame():
            endGame();
            return;
        }

        // clean up is necessary, so we call leaveRoom() and wait until we get the callback
        // in order to call endGame():
        String roomId = mRoom.getRoomId();
        mRoom = null;

        if (mGameHelper.getGamesClient().isConnected()) {
            debugLog("Leaving room " + roomId);
            setState(MP_LEAVING);
            debugLog("Waiting for onLeftRoom callback in order to officially end game.");
            mGameHelper.getGamesClient().leaveRoom(this, roomId);
            // do not call endGame() here, do that from the callback when we know we've left
            // the room
        } else {
            debugLog("GamesClient is no longer connected, so we have left the room.");
            endGame();
        }
    }

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        debugLog("Room created, status " + statusCode);
        if (statusCode != GamesClient.STATUS_OK) {
            logError("Room creation error: " + statusCode);
            shutdownGame(END_REASON_ERROR);
            return;
        }

        // we are now in the MP_WAITING state
        setState(MP_WAITING);
        updateRoom(room);

        // show the waiting room
        showWaitingRoom();
    }

    String participantStatusToString(int status) {
        switch (status) {
            case Participant.STATUS_LEFT:
                return "STATUS_LEFT";
            case Participant.STATUS_DECLINED:
                return "STATUS_DECLINED";
            case Participant.STATUS_INVITED:
                return "STATUS_INVITED";
            case Participant.STATUS_JOINED:
                return "STATUS_JOINED";
            default:
                return "unknown status: " + status;
        }

    }

    String roomStatusToString(int status) {
        switch (status) {
            case Room.ROOM_STATUS_CONNECTING:
                return "ROOM_STATUS_CONNECTING";
            case Room.ROOM_STATUS_ACTIVE:
                return "ROOM_STATUS_ACTIVE";
            case Room.ROOM_STATUS_AUTO_MATCHING:
                return "ROOM_STATUS_AUTO_MATCHING";
            case Room.ROOM_STATUS_INVITING:
                return "ROOM_STATUS_INVITING";
            default:
                return "(room status: " + status + ")";
        }
    }
    void logRoom(Room room) {
        if (room == null) {
            debugLog("Room: null");
            return;
        }
        debugLog("Room: " + room.getRoomId());
        debugLog("Status: " + roomStatusToString(room.getStatus()));
        debugLog("Participants (" + room.getParticipants().size() + "):");
        for (Participant p : room.getParticipants()) {
            debugLog("  * " + p.getDisplayName() + " (" + participantStatusToString(
                    p.getStatus()) + "), " + (p.isConnectedToRoom() ? "CONNECTED" :
                    "not connected"));
        }
    }

    void updateRoom(Room room) {
        debugLog("Updating room.");
        mRoom = room;
        logRoom(mRoom);

        // determine my ID
        mMyId = mRoom.getParticipantId(mGameHelper.getGamesClient().getCurrentPlayerId());
        debugLog("My ID: " + mMyId);

        // recompute list of active participants
        List<Participant> newList = new ArrayList<Participant>();
        List<Participant> addedParticipants = new ArrayList<Participant>();
        List<Participant> droppedParticipants = new ArrayList<Participant>();

        for (Participant p : room.getParticipants()) {
            if (p.getStatus() == Participant.STATUS_JOINED && p.isConnectedToRoom()) {
                newList.add(p);
            }
        }

        // figure out who was dropped
        for (Participant p : mConnectedParticipants) {
            if (!newList.contains(p)) {
                droppedParticipants.add(p);
            }
        }

        // figure out who was added
        for (Participant p : newList) {
            if (!mConnectedParticipants.contains(p)) {
                addedParticipants.add(p);
            }
        }

        mConnectedParticipants = newList;

        // recompute list of "participants excluding self"
        mConnectedParticipantsExcludingSelf.clear();
        for (Participant p : mConnectedParticipants) {
            if (!p.getParticipantId().equals(mMyId)) {
                mConnectedParticipantsExcludingSelf.add(p);
            }
        }

        debugLog(addedParticipants.size() + " participants were just added");
        debugLog(droppedParticipants.size() + " participants were just dropped");

        if (mRtmpListener != null) {
            for (Participant p : droppedParticipants) {
                debugLog("Reporting player dropped: " + p.getParticipantId() + " " +
                        p.getDisplayName());
                mRtmpListener.onRtmpDropped(p);
            }
            for (Participant p : addedParticipants) {
                debugLog("Reporting player added: " + p.getParticipantId() + " " +
                        p.getDisplayName());
                mRtmpListener.onRtmpAdded(p);
            }
        }
    }

    boolean amIConnectedToRoom() {
        for (Participant p : mRoom.getParticipants()) {
            if (p.getParticipantId().equals(mMyId) && p.getStatus() == Participant.STATUS_JOINED
                    && p.isConnectedToRoom()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        debugLog("Joined room, status " + statusCode);
        if (statusCode != GamesClient.STATUS_OK) {
            logError("Room join error: " + statusCode);
            shutdownGame(END_REASON_ERROR);
            return;
        }

        // we are now in the MP_WAITING state
        setState(MP_WAITING);

        updateRoom(room);
        showWaitingRoom();
    }

    void showWaitingRoom() {
        assertHasActivityAndListener("showWaitingRoom");
        assertState("showWaitingRoom", MP_WAITING);
        mWaitRoomDismissedFromCode = false;

        Intent i = mGameHelper.getGamesClient().getRealTimeWaitingRoomIntent(mRoom,
                mStartEarly ? mMinPlayers : Integer.MAX_VALUE);
        mExpectedActivityResult = EXPECTING_WAITING_ROOM;
        mGameHelper.mActivity.startActivityForResult(i, GameHelper.RC_MULTIPLAYER);
    }

    void dismissWaitingRoom() {
        if (mExpectedActivityResult != EXPECTING_WAITING_ROOM) {
            debugLog("Not dismissing waiting room because it's not onscreen.");
            return;
        }
        if (mGameHelper.mActivity == null) {
            debugLog("No waiting room to dismiss (no Activity)");
            return;
        }
        mWaitRoomDismissedFromCode = true;
        debugLog("Dismissing waiting room.");
        mGameHelper.mActivity.finishActivity(GameHelper.RC_MULTIPLAYER);
    }

    void debugLog(String msg) {
        mGameHelper.debugLog(msg);
    }

    void logError(String error) {
        Log.e(mGameHelper.mDebugTag, "*** RtmpHelper: " + error);
    }

    void logWarn(String warning) {
        Log.w(mGameHelper.mDebugTag, "!!! RtmpHelper: " + warning);
    }

    @Override
    public void onRoomConnecting(Room room) {
        debugLog("Room is connecting...");
        updateRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        debugLog("Room is auto matching...");
        updateRoom(room);
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> strings) {
        debugLog("Peers invited to room: " + strings);
        updateRoom(room);
    }

    @Override
    public void onPeerDeclined(Room room, List<String> strings) {
        debugLog("Peers declined invitation: " + strings);
        updateRoom(room);
    }

    @Override
    public void onPeerJoined(Room room, List<String> strings) {
        debugLog("Peers joined: " + strings);
        updateRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> strings) {
        debugLog("Peers left: " + strings);
        updateRoom(room);
    }

    @Override
    public void onConnectedToRoom(Room room) {
        debugLog("Connected to room.");
        updateRoom(room);
    }

    @Override
    public void onDisconnectedFromRoom(Room room) {
        debugLog("Disconnected from room.");
        updateRoom(room);

        if (mMpState == MP_LEAVING) {
            debugLog("Disconnection is expected (we're leaving). Ignoring.");
        } else {
            logError("Unexpected disconnection from room. Cleaning up.");
            shutdownGame(END_REASON_ERROR);
        }
    }

    @Override
    public void onPeersConnected(Room room, List<String> strings) {
        debugLog("Peers connected: " + strings);
        updateRoom(room);
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> strings) {
        debugLog("Peers disconnected: " + strings);
        updateRoom(room);
    }

    @Override
    public void onLeftRoom(int statusCode, String s) {
        debugLog("Left room, status " + statusCode);

        if (mMpState == MP_LEAVING) {
            debugLog("Departure from room is expected (we're leaving).");
            debugLog("Ending game and calling callback.");
            endGame();
        } else {
            logError("Unexpected departure from room. Cleaning up.");
            shutdownGame(END_REASON_ERROR);
        }
    }

    @Override
    public void onRoomConnected(int statusCode, Room room) {
        debugLog("Room connected, status " + statusCode);
        updateRoom(room);

        if (statusCode != GamesClient.STATUS_OK) {
            logError("onRoomConnected error: " + statusCode);
            shutdownGame(END_REASON_ERROR);
            return;
        }
    }

    @Override
    public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
        if (mRtmpListener != null) {
            mRtmpListener.onRtmpMessage(realTimeMessage);
        }
    }

    @Override
    public void onInvitationReceived(Invitation invitation) {
        debugLog("Received invitation " + invitation + ", informing listener.");
        if (mRtmpListener != null) {
            mRtmpListener.onRtmpInvitation(invitation);
        }
    }
}
