package ru.gravit.launchserver.auth.provider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ru.gravit.launcher.serialize.config.entry.BooleanConfigEntry;
import ru.gravit.launchserver.LaunchServer;
import ru.gravit.launchserver.auth.ClientPermissions;
import ru.gravit.utils.helper.CommonHelper;
import ru.gravit.utils.helper.SecurityHelper;
import ru.gravit.utils.helper.VerifyHelper;
import ru.gravit.launcher.serialize.config.entry.BlockConfigEntry;
import ru.gravit.launcher.serialize.config.entry.ListConfigEntry;
import ru.gravit.launcher.serialize.config.entry.StringConfigEntry;
import ru.gravit.launchserver.auth.AuthException;
import ru.gravit.launchserver.auth.MySQLSourceConfig;

public final class MySQLAuthProvider extends AuthProvider {
    private final MySQLSourceConfig mySQLHolder;
    private final String query;
    private final String[] queryParams;
    private final boolean usePermission;

    public MySQLAuthProvider(BlockConfigEntry block, LaunchServer server) {
        super(block,server);
        mySQLHolder = new MySQLSourceConfig("authProviderPool", block);

        // Read query
        query = VerifyHelper.verify(block.getEntryValue("query", StringConfigEntry.class),
                VerifyHelper.NOT_EMPTY, "MySQL query can't be empty");
        usePermission = block.hasEntry("usePermission") ? block.getEntryValue("usePermission", BooleanConfigEntry.class) : false;
        queryParams = block.getEntry("queryParams", ListConfigEntry.class).
                stream(StringConfigEntry.class).toArray(String[]::new);
    }

    @Override
    public AuthProviderResult auth(String login, String password, String ip) throws SQLException, AuthException {
        Connection c = mySQLHolder.getConnection();
        PreparedStatement s = c.prepareStatement(query);
        String[] replaceParams = {"login", login, "password", password, "ip", ip};
        for (int i = 0; i < queryParams.length; i++)
            s.setString(i + 1, CommonHelper.replace(queryParams[i], replaceParams));

        // Execute SQL query
        s.setQueryTimeout(MySQLSourceConfig.TIMEOUT);
        try (ResultSet set = s.executeQuery()) {
            return set.next() ? new AuthProviderResult(set.getString(1), SecurityHelper.randomStringToken(), usePermission ? new ClientPermissions(set.getLong(2)) : new ClientPermissions()) : authError("Incorrect username or password");
        }
    }

    @Override
    public void close() {
        // Do nothing
    }
}
