package org.telegram.ui;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MusicPlaylistStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

public class MusicActivity extends BaseFragment implements MainTabsActivity.TabFragmentDelegate, NotificationCenter.NotificationCenterDelegate {

    private TextView playerTitle;
    private TextView playerSubtitle;
    private TextView playPauseButton;
    private EditText channelsSearchField;
    private LinearLayout recommendationsCard;
    private LinearLayout playlistsCard;
    private MusicPlaylistStorage playlistStorage;

    public MusicActivity(Bundle args) {
        super(args);
    }

    @Override
    public View createView(android.content.Context context) {
        actionBar.setTitle(LocaleController.getString(R.string.MainTabsMusic));
        playlistStorage = new MusicPlaylistStorage(currentAccount);

        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(120));
        scrollView.addView(content, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));

        content.addView(createSectionTitle(context, R.string.MusicTelegramPlayerTitle));
        LinearLayout playerCard = createCard(context);
        playerTitle = createCardTitle(context, R.string.MusicTelegramPlayerEmptyTitle);
        playerSubtitle = createCardSubtitle(context, R.string.MusicTelegramPlayerEmptySubtitle);
        playPauseButton = createActionButton(context, R.string.MusicTelegramPlayerPlay);
        playPauseButton.setOnClickListener(v -> togglePlayback());
        TextView addToFavorites = createActionButton(context, R.string.MusicAddToFavoritesAction);
        addToFavorites.setOnClickListener(v -> addCurrentTrackToPlaylist(LocaleController.getString(R.string.MusicFavoritesPlaylist)));
        playerCard.addView(playerTitle);
        playerCard.addView(playerSubtitle);
        playerCard.addView(playPauseButton);
        playerCard.addView(addToFavorites);
        content.addView(playerCard);

        content.addView(createSectionTitle(context, R.string.MusicRecommendationsTitle));
        recommendationsCard = createCard(context);
        content.addView(recommendationsCard);

        content.addView(createSectionTitle(context, R.string.MusicChannelsSearchTitle));
        LinearLayout channelsCard = createCard(context);
        channelsSearchField = new EditText(context);
        channelsSearchField.setHint(LocaleController.getString(R.string.MusicChannelsSearchHint));
        channelsSearchField.setInputType(InputType.TYPE_CLASS_TEXT);
        channelsSearchField.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        channelsSearchField.setHintTextColor(getThemedColor(Theme.key_windowBackgroundWhiteHintText));
        channelsCard.addView(channelsSearchField, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        TextView searchButton = createActionButton(context, R.string.MusicChannelsSearchAction);
        searchButton.setOnClickListener(v -> openPublicChannelsSearch());
        channelsCard.addView(searchButton);
        content.addView(channelsCard);

        content.addView(createSectionTitle(context, R.string.MusicPlaylistsTitle));
        playlistsCard = createCard(context);
        TextView createPlaylistButton = createActionButton(context, R.string.MusicCreatePlaylistAction);
        createPlaylistButton.setOnClickListener(v -> showCreatePlaylistDialog());
        playlistsCard.addView(createPlaylistButton);
        content.addView(playlistsCard);

        content.addView(createSectionTitle(context, R.string.MusicLastfmTitle));
        LinearLayout lastFmCard = createCard(context);
        lastFmCard.addView(createCardSubtitle(context, R.string.MusicLastfmHint));
        TextView oauthButton = createActionButton(context, R.string.MusicLastfmConnectAction);
        oauthButton.setOnClickListener(v -> openLink("https://www.last.fm/api/auth"));
        lastFmCard.addView(oauthButton);
        content.addView(lastFmCard);

        content.addView(createSectionTitle(context, R.string.MusicArtistsTitle));
        LinearLayout artistsCard = createCard(context);
        artistsCard.addView(createArtistButton(context, "Daft Punk"));
        artistsCard.addView(createArtistButton(context, "Billie Eilish"));
        artistsCard.addView(createArtistButton(context, "Radiohead"));
        content.addView(artistsCard);

        fragmentView = frameLayout;
        updateAllDynamicSections();
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getNotificationCenter().addObserver(this, NotificationCenter.messagePlayingDidStart);
        getNotificationCenter().addObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        getNotificationCenter().addObserver(this, NotificationCenter.messagePlayingDidReset);
        updateAllDynamicSections();
    }

    @Override
    public void onPause() {
        super.onPause();
        getNotificationCenter().removeObserver(this, NotificationCenter.messagePlayingDidStart);
        getNotificationCenter().removeObserver(this, NotificationCenter.messagePlayingPlayStateChanged);
        getNotificationCenter().removeObserver(this, NotificationCenter.messagePlayingDidReset);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.messagePlayingDidStart || id == NotificationCenter.messagePlayingPlayStateChanged || id == NotificationCenter.messagePlayingDidReset) {
            updateAllDynamicSections();
        }
    }

    private void updateAllDynamicSections() {
        updatePlayerState();
        updateRecommendations();
        updatePlaylists();
    }

    private void togglePlayback() {
        MediaController mediaController = MediaController.getInstance();
        MessageObject messageObject = mediaController.getPlayingMessageObject();
        if (messageObject == null) {
            AndroidUtilities.shakeView(playPauseButton);
            return;
        }
        if (mediaController.isMessagePaused()) {
            mediaController.playMessage(messageObject);
        } else {
            mediaController.pauseMessage(messageObject);
        }
        updatePlayerState();
    }

    private void updatePlayerState() {
        if (playerTitle == null) {
            return;
        }
        MessageObject messageObject = MediaController.getInstance().getPlayingMessageObject();
        if (messageObject == null) {
            playerTitle.setText(LocaleController.getString(R.string.MusicTelegramPlayerEmptyTitle));
            playerSubtitle.setText(LocaleController.getString(R.string.MusicTelegramPlayerEmptySubtitle));
            playPauseButton.setText(LocaleController.getString(R.string.MusicTelegramPlayerPlay));
            return;
        }

        String title = messageObject.getMusicTitle();
        if (TextUtils.isEmpty(title)) {
            title = LocaleController.getString(R.string.AudioUnknownArtist);
        }
        String author = messageObject.getMusicAuthor();
        if (TextUtils.isEmpty(author)) {
            author = LocaleController.getString(R.string.AudioUnknownArtist);
        }

        playerTitle.setText(title);
        playerSubtitle.setText(author);
        playPauseButton.setText(LocaleController.getString(MediaController.getInstance().isMessagePaused() ? R.string.MusicTelegramPlayerPlay : R.string.MusicTelegramPlayerPause));
    }

    private void updateRecommendations() {
        if (recommendationsCard == null) {
            return;
        }
        recommendationsCard.removeAllViews();
        recommendationsCard.addView(createCardSubtitle(getContext(), R.string.MusicRecommendationsHint));

        ArrayList<MessageObject> playlist = MediaController.getInstance().getPlaylist();
        int shown = 0;
        for (int i = Math.max(0, playlist.size() - 1); i >= 0 && shown < 5; i--) {
            MessageObject object = playlist.get(i);
            if (object == null || !object.isMusic()) {
                continue;
            }
            recommendationsCard.addView(createTrackLine(getContext(), object.getMusicTitle(), object.getMusicAuthor()));
            shown++;
        }

        if (shown == 0) {
            recommendationsCard.addView(createTrackLine(getContext(), LocaleController.getString(R.string.MusicNoRecommendations), ""));
        }
    }

    private void updatePlaylists() {
        if (playlistsCard == null) {
            return;
        }

        while (playlistsCard.getChildCount() > 1) {
            playlistsCard.removeViewAt(1);
        }

        ArrayList<MusicPlaylistStorage.Playlist> playlists = playlistStorage.getPlaylists();
        if (playlists.isEmpty()) {
            playlistsCard.addView(createTrackLine(getContext(), LocaleController.getString(R.string.MusicNoPlaylists), ""));
            return;
        }

        for (int i = 0; i < playlists.size(); i++) {
            MusicPlaylistStorage.Playlist playlist = playlists.get(i);
            String subtitle = LocaleController.formatPluralString("Files", playlist.tracks.size());
            playlistsCard.addView(createTrackLine(getContext(), playlist.name, subtitle));
        }
    }

    private void addCurrentTrackToPlaylist(String playlistName) {
        MessageObject playing = MediaController.getInstance().getPlayingMessageObject();
        if (playing == null) {
            AndroidUtilities.shakeView(playPauseButton);
            return;
        }
        playlistStorage.addTrack(playlistName, playing);
        updatePlaylists();
    }

    private void showCreatePlaylistDialog() {
        if (getParentActivity() == null) {
            return;
        }
        final EditText input = new EditText(getParentActivity());
        input.setHint(LocaleController.getString(R.string.MusicCreatePlaylistHint));

        AlertDialog dialog = new AlertDialog.Builder(getParentActivity())
            .setTitle(LocaleController.getString(R.string.MusicCreatePlaylistAction))
            .setView(input)
            .setPositiveButton(LocaleController.getString(R.string.Create), (d, w) -> {
                String name = input.getText() != null ? input.getText().toString().trim() : "";
                if (TextUtils.isEmpty(name)) {
                    return;
                }
                playlistStorage.ensurePlaylist(name);
                updatePlaylists();
            })
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
            .create();
        dialog.show();
    }

    private void openPublicChannelsSearch() {
        String query = channelsSearchField != null ? channelsSearchField.getText().toString().trim() : "";
        if (TextUtils.isEmpty(query)) {
            query = "music";
        }
        openLink("https://t.me/s/" + Uri.encode(query));
    }

    private void openLink(String link) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
            getParentActivity().startActivity(intent);
        } catch (ActivityNotFoundException ignore) {
        }
    }

    private TextView createSectionTitle(android.content.Context context, int stringRes) {
        TextView title = new TextView(context);
        title.setText(LocaleController.getString(stringRes));
        title.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(16);
        title.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(8));
        return title;
    }

    private LinearLayout createCard(android.content.Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(14), getThemedColor(Theme.key_windowBackgroundWhite)));
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14));
        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        lp.bottomMargin = AndroidUtilities.dp(10);
        card.setLayoutParams(lp);
        return card;
    }

    private TextView createCardTitle(android.content.Context context, int stringRes) {
        TextView tv = new TextView(context);
        tv.setText(LocaleController.getString(stringRes));
        tv.setTextSize(16);
        tv.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private TextView createCardSubtitle(android.content.Context context, int stringRes) {
        TextView tv = new TextView(context);
        tv.setText(LocaleController.getString(stringRes));
        tv.setTextSize(14);
        tv.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        tv.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(8));
        return tv;
    }

    private TextView createActionButton(android.content.Context context, int stringRes) {
        TextView button = new TextView(context);
        button.setText(LocaleController.getString(stringRes));
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(getThemedColor(Theme.key_featuredStickers_buttonText));
        button.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(10), getThemedColor(Theme.key_featuredStickers_addButton)));
        button.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(9), AndroidUtilities.dp(12), AndroidUtilities.dp(9));
        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        lp.topMargin = AndroidUtilities.dp(6);
        button.setLayoutParams(lp);
        return button;
    }

    private View createTrackLine(android.content.Context context, String title, String subtitle) {
        LinearLayout line = new LinearLayout(context);
        line.setOrientation(LinearLayout.VERTICAL);
        line.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));

        TextView titleView = new TextView(context);
        titleView.setText(TextUtils.isEmpty(title) ? LocaleController.getString(R.string.AudioUnknownTitle) : title);
        titleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(14);
        line.addView(titleView);

        if (!TextUtils.isEmpty(subtitle)) {
            TextView subtitleView = new TextView(context);
            subtitleView.setText(subtitle);
            subtitleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
            subtitleView.setTextSize(13);
            line.addView(subtitleView);
        }
        return line;
    }

    private TextView createArtistButton(android.content.Context context, String artistName) {
        TextView artist = new TextView(context);
        artist.setText(artistName);
        artist.setTextSize(15);
        artist.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText));
        artist.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        artist.setOnClickListener(v -> openLink("https://www.last.fm/music/" + Uri.encode(artistName)));
        return artist;
    }
}
