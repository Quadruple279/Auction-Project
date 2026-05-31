
package shared.protocol;
public enum MessageType {
    // ── Client → Server ──────────────────────────
    LOGIN,          // payload: username, password
    REGISTER,       // payload: username, password, role
    BID,            // payload: auctionId, amount
    SUBSCRIBE,      // payload: auctionId
    UNSUBSCRIBE,    // payload: auctionId
    LOGOUT,
    GET_AUCTIONS,
    CREATE_AUCTION,   // payload: itemType, itemName, description, price, durationMinutes, info1, info2
    DELETE_AUCTION,   // payload: auctionId
    UPDATE_AUCTION,   // payload: auctionId, newName, newDescription, newPrice
    ENABLE_AUTO_BID,  // payload: auctionId, maxBid, increment
    DISABLE_AUTO_BID,  // payload: auctionId
    UPDATE_USER,         // Client → Server: payload: newName, newPassword
    FINISH_AUCTION,      // payload: auctionId
    CANCEL_AUCTION,      // payload: auctionId
    MARK_PAID,           // payload: auctionId
    GET_BID_HISTORY,
    GET_USERS,              // Admin lấy danh sách tất cả users
    DELETE_USER,            // payload: username
    ADD_USER,               // payload: username, password, role
    UPDATE_USER_ADMIN,      // payload: targetUsername, newDisplayName, newPassword
    GET_BID_HISTORY_BY_USER, // payload: username (dùng cho ProfileController)
    GET_SYSTEM_LOG,

    // ── Server → Client ──────────────────────────
    LOGIN_SUCCESS,  // payload: username, role
    LOGIN_FAILED,   // payload: reason
    REGISTER_SUCCESS,
    REGISTER_FAILED,
    AUCTION_UPDATE, // payload: auctionId, currentPrice, leadingBidder, eventType
    AUCTION_LIST,   // payload: data (JSON array dạng string)
    ERROR,CREATE_AUCTION_SUCCESS,  // payload: auctionId
    DELETE_AUCTION_SUCCESS,  // payload: auctionId
    UPDATE_AUCTION_SUCCESS,  // (không cần payload)
    NEW_AUCTION,             // broadcast đến tất cả client: payload: auctionId
    GET_BID_HISTORY_SUCCESS,
    USER_LIST,                   // payload: data (JSON array UserDTO)
    USER_DELETED,                // broadcast — payload: username
    ADD_USER_SUCCESS,            // payload: username
    UPDATE_USER_ADMIN_SUCCESS,   // không cần payload
    GET_BID_HISTORY_BY_USER_SUCCESS, // payload: data (JSON array)
    GET_SYSTEM_LOG_SUCCESS,   // payload: data (JSON array SystemLogDTO)


    // payload: reason
    UPDATE_USER_SUCCESS,
    FINISH_AUCTION_SUCCESS,
    CANCEL_AUCTION_SUCCESS,
    MARK_PAID_SUCCESS,
}
