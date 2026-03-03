package org.telegram.messenger;

import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MusicPlaylistStorage {

    private static final String PREFS = "music_playlists";
    private static final String KEY_PLAYLISTS = "playlists_json";

    public static class PlaylistTrack {
        public long dialogId;
        public int messageId;
        public String title;
        public String artist;

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("dialogId", dialogId);
                json.put("messageId", messageId);
                json.put("title", title);
                json.put("artist", artist);
            } catch (Exception ignore) {
            }
            return json;
        }

        public static PlaylistTrack fromJson(JSONObject json) {
            PlaylistTrack track = new PlaylistTrack();
            if (json == null) {
                return track;
            }
            track.dialogId = json.optLong("dialogId");
            track.messageId = json.optInt("messageId");
            track.title = json.optString("title");
            track.artist = json.optString("artist");
            return track;
        }
    }

    public static class Playlist {
        public String name;
        public ArrayList<PlaylistTrack> tracks = new ArrayList<>();

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("name", name);
                JSONArray arr = new JSONArray();
                for (int i = 0; i < tracks.size(); i++) {
                    arr.put(tracks.get(i).toJson());
                }
                json.put("tracks", arr);
            } catch (Exception ignore) {
            }
            return json;
        }

        public static Playlist fromJson(JSONObject json) {
            Playlist playlist = new Playlist();
            if (json == null) {
                return playlist;
            }
            playlist.name = json.optString("name");
            JSONArray arr = json.optJSONArray("tracks");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    playlist.tracks.add(PlaylistTrack.fromJson(arr.optJSONObject(i)));
                }
            }
            return playlist;
        }
    }

    private final SharedPreferences preferences;

    public MusicPlaylistStorage(int account) {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences(PREFS + "_" + account, 0);
    }

    public ArrayList<Playlist> getPlaylists() {
        ArrayList<Playlist> result = new ArrayList<>();
        String raw = preferences.getString(KEY_PLAYLISTS, "");
        if (TextUtils.isEmpty(raw)) {
            return result;
        }
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                result.add(Playlist.fromJson(arr.optJSONObject(i)));
            }
        } catch (Exception ignore) {
        }
        return result;
    }

    public void savePlaylists(List<Playlist> playlists) {
        JSONArray arr = new JSONArray();
        for (Playlist playlist : playlists) {
            arr.put(playlist.toJson());
        }
        preferences.edit().putString(KEY_PLAYLISTS, arr.toString()).apply();
    }

    public Playlist ensurePlaylist(String name) {
        ArrayList<Playlist> playlists = getPlaylists();
        for (Playlist playlist : playlists) {
            if (name.equals(playlist.name)) {
                return playlist;
            }
        }
        Playlist playlist = new Playlist();
        playlist.name = name;
        playlists.add(playlist);
        savePlaylists(playlists);
        return playlist;
    }

    public void addTrack(String playlistName, MessageObject object) {
        if (object == null) {
            return;
        }
        ArrayList<Playlist> playlists = getPlaylists();
        Playlist target = null;
        for (Playlist playlist : playlists) {
            if (playlistName.equals(playlist.name)) {
                target = playlist;
                break;
            }
        }
        if (target == null) {
            target = new Playlist();
            target.name = playlistName;
            playlists.add(target);
        }

        for (int i = 0; i < target.tracks.size(); i++) {
            PlaylistTrack track = target.tracks.get(i);
            if (track.messageId == object.getId() && track.dialogId == object.getDialogId()) {
                return;
            }
        }

        PlaylistTrack track = new PlaylistTrack();
        track.messageId = object.getId();
        track.dialogId = object.getDialogId();
        track.title = object.getMusicTitle();
        track.artist = object.getMusicAuthor();
        target.tracks.add(0, track);
        savePlaylists(playlists);
    }
}
