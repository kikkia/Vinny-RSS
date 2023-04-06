package com.rss.db.dao;

import com.rss.db.model.RssChannelSubscriptionDTO;
import com.rss.db.model.RssSubscriptionDTO;
import com.rss.utils.DislogLogger;
import com.rss.model.RssProvider;
import com.rss.utils.RssUtils;
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
        String getQuery = "SELECT * FROM rss_subscription WHERE lastScanAttempt < ? AND provider = ? order by lastScanAttempt asc LIMIT 1;";
        String updateQuery = "UPDATE rss_subscription SET lastScanAttempt = ? WHERE id = ?";

        RssSubscriptionDTO dto;

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(getQuery)) {
                statement.setLong(1, System.currentTimeMillis() - RssUtils.Companion.getMinIntervalForProvider(provider));
                statement.setInt(2, provider.getValue());
                try(ResultSet set = statement.executeQuery()) {
                    if (!set.first())
                        return null;

                    dto = new RssSubscriptionDTO(
                            set.getInt("id"),
                            set.getString("subject"),
                            set.getInt("provider"),
                            Instant.ofEpochMilli(set.getLong("lastScanAttempt")),
                            Instant.ofEpochMilli(set.getLong("lastScanComplete")),
                            set.getString("display_name")
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
                                set.getString("text_channel_id"),
                                set.getString("keyword")
                        ));
                    }
                }
            }
        }
        return channels;
    }

    public boolean updateLastScanComplete(int id) {
        String query = "UPDATE rss_subscription SET lastScanComplete = ? WHERE id = ?";

        // Update the last scan attempt
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setLong(1, System.currentTimeMillis());
                statement.setInt(2, id);
                statement.execute();
            }
        } catch (SQLException e) {
            logger.error("Failed to update lastScanComplete", e);
            return false;
        }
        return true;
    }

    public boolean updateDisplayName(int id, String newDisplayName) {
        String query = "UPDATE rss_subscription SET display_name = ? WHERE id = ?";
        // Update the last scan attempt
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, newDisplayName);
                statement.setInt(2, id);
                statement.execute();
            }
        } catch (SQLException e) {
            logger.error("Failed to update display_name", e);
            return false;
        }
        return true;
    }

    public void delete(int id) {
        String deleteChannelSubsQuery = "DELETE FROM channel_rss_subscription WHERE rss_subscription_id = ?";
        String deleteSubQuery = "DELETE FROM rss_subscription WHERE id = ?";

        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(deleteChannelSubsQuery)) {
                statement.setInt(1, id);
                statement.execute();
            }
            try (PreparedStatement statement = connection.prepareStatement(deleteSubQuery)) {
                statement.setInt(1, id);
                statement.execute();
            }
        } catch (SQLException e) {
            logger.error("Failed to delete subscription", e);
        }
    }
}
