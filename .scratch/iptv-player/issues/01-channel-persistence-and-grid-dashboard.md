# 01: Channel Persistence & Grid Dashboard

**What to build:** A user opening the application sees a clean channel grid or an empty state call-to-action when no channels exist. The user can tap an "Add Channel" action to open an input form, type a channel name and an M3U8 stream URL, and submit. If inputs are invalid (empty name or malformed URL), validation messages guide the user. Valid submissions save the channel to local persistent storage and immediately display the new channel tile in the grid with a default placeholder logo. The list and channels remain intact across application restarts.

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] Empty state with clear call-to-action is displayed when no channels have been added yet
- [ ] Tapping the add button opens a form allowing the user to input a channel name and stream URL
- [ ] Input validation prevents saving with empty channel names or invalid URLs, showing clear feedback
- [ ] Saving a valid channel adds it to local storage and updates the grid view immediately
- [ ] Channels in the grid display the channel name and a clean default placeholder logo
- [ ] Stored channels persist and display correctly across app restarts
- [ ] End-to-end UI screen tests verify the channel creation and grid display flows

