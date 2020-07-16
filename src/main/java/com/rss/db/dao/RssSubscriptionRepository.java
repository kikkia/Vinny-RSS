package com.rss.db.dao;

import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.utils.DislogLogger;
import com.rss.model.RssProvider;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RssSubscriptionRepository {

    private DislogLogger logger = new DislogLogger(this.getClass());

    @Autowired
    private HikariDataSource dataSource;

    public RssSubscriptionDTO getNextSubscription(RssProvider provider) {
        String getQuery = "SELECT * FROM rss_subscription WHERE lastScanAttempted < ? AND provider = ? order by lastScanAttempted asc LIMIT 1;";
        String updateQuery = "UPDATE rss_subscriptions SET lastScanAttempted = ? WHERE id = ?";

        RssSubscriptionDTO dto;

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(getQuery)) {
                statement.setLong(1, System.currentTimeMillis() - 5000);
                statement.setInt(2, provider.getValue());
                try(ResultSet set = statement.executeQuery()) {
                    if (!set.first())
                        return null;

                    dto = new RssSubscriptionDTO(
                            set.getInt("id"),
                            set.getString("url"),
                            set.getInt("provider"),
                            Instant.ofEpochMilli(set.getLong("lastScanAttempt")),
                            Instant.ofEpochMilli(set.getLong("lastScanCompleted"))
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to get next subscription " + provider.getValue(), e);
            return null;
        }

        // Update the last scan attempt
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(updateQuery)) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setInt(2, dto.getId());
                statement.execute();
            }
        } catch (SQLException e) {
            logger.error("Failed to update lastScanAttempt", e);
            return null;
        }
        return dto;
    }

    public List<RssChannelSubscriptionDTO> getChannelSubsForSubcriptionId(int subscriptionId) throws SQLException {
        String getQuery = "SELECT * FROM channel_rss_subscription s WHERE s.rss_subscription_id = ?";

        List<RssChannelSubscriptionDTO> channels = new ArrayList<>();
        try(Connection connection = dataSource.getConnection()) {
            try(PreparedStatement statement = connection.prepareStatement(getQuery)) {
                statement.setInt(1, subscriptionId);
                try (ResultSet set = statement.executeQuery()){
                    while (set.next()) {
                        channels.add(new RssChannelSubscriptionDTO(
                                set.getInt("id"),
                                set.getInt("rss_subscription_id"),
                                set.getString("channel_id"),
                                set.getString("keyword")
                        ));
                    }
                }
            }
        }
        return channels;
    }

}
