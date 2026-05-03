
package shared.protocol;
public enum MessageType {
    // ── Client → Server ──────────────────────────
    LOGIN,          // payload: username, password
    REGISTER,       // payload: username, password, role
    BID,            // payload: auctionId, amount
    SUBSCRIBE,      // payload: auctionId
    UNSUBSCRIBE,    // payload: auctionId
    LOGOUT,

    // ── Server → Client ──────────────────────────
    LOGIN_SUCCESS,  // payload: username, role
    LOGIN_FAILED,   // payload: reason
    REGISTER_SUCCESS,
    REGISTER_FAILED,
    AUCTION_UPDATE, // payload: auctionId, currentPrice, leadingBidder, eventType
    AUCTION_LIST,   // payload: data (JSON array dạng string)
    ERROR           // payload: reason
}
