
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
    NEW_AUCTION             // broadcast đến tất cả client: payload: auctionId
    // payload: reason
}
