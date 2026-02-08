package io.kestra.plugin.slack.app.models;

import io.kestra.plugin.slack.services.MessageService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

import static io.kestra.plugin.slack.services.MessageService.fromSlackTimestamp;

@Value
@Builder
@Jacksonized
public class UserOutput implements io.kestra.core.models.tasks.Output {
    @Schema(title = "The user ID.")
    String id;

    @Schema(title = "The team ID.")
    String teamId;

    @Schema(title = "The user name.")
    String name;

    @Schema(title = "Whether the user is deleted.")
    Boolean deleted;

    @Schema(title = "The user's color.")
    String color;

    @Schema(title = "The user's real name.")
    String realName;

    @Schema(title = "The user's timezone.")
    String tz;

    @Schema(title = "The user's timezone label.")
    String tzLabel;

    @Schema(title = "The user's timezone offset.")
    Integer tzOffset;

    @Schema(title = "The user profile information.")
    UserProfile profile;

    @Schema(title = "Whether the user is an admin.")
    Boolean isAdmin;

    @Schema(title = "Whether the user is an owner.")
    Boolean isOwner;

    @Schema(title = "Whether the user is a primary owner.")
    Boolean isPrimaryOwner;

    @Schema(title = "Whether the user is restricted.")
    Boolean isRestricted;

    @Schema(title = "Whether the user is ultra restricted.")
    Boolean isUltraRestricted;

    @Schema(title = "Whether the user is a bot.")
    Boolean isBot;

    @Schema(title = "Whether the user is an app user.")
    Boolean isAppUser;

    @Schema(title = "The timestamp when the user was last updated.")
    Instant updated;

    @Schema(title = "Whether the user has 2FA enabled.")
    Boolean has2fa;

    public static UserOutput of(com.slack.api.model.User user) {
        if (user == null) {
            return null;
        }

        return UserOutput.builder()
            .id(user.getId())
            .teamId(user.getTeamId())
            .name(user.getName())
            .deleted(user.isDeleted())
            .color(user.getColor())
            .realName(user.getRealName())
            .tz(user.getTz())
            .tzLabel(user.getTzLabel())
            .tzOffset(user.getTzOffset())
            .profile(UserProfile.of(user.getProfile()))
            .isAdmin(user.isAdmin())
            .isOwner(user.isOwner())
            .isPrimaryOwner(user.isPrimaryOwner())
            .isRestricted(user.isRestricted())
            .isUltraRestricted(user.isUltraRestricted())
            .isBot(user.isBot())
            .isAppUser(user.isAppUser())
            .updated(MessageService.fromSlackTimestamp(user.getUpdated()))
            .has2fa(user.isHas2fa())
            .build();
    }

    @Value
    @Builder
    @Jacksonized
    public static class UserProfile {
        @Schema(title = "The user's title.")
        String title;

        @Schema(title = "The user's phone number.")
        String phone;

        @Schema(title = "The user's Skype name.")
        String skype;

        @Schema(title = "The user's real name.")
        String realName;

        @Schema(title = "The normalized real name.")
        String realNameNormalized;

        @Schema(title = "The user's display name.")
        String displayName;

        @Schema(title = "The normalized display name.")
        String displayNameNormalized;

        @Schema(title = "The user's status text.")
        String statusText;

        @Schema(title = "The user's status emoji.")
        String statusEmoji;

        @Schema(title = "The user's status expiration timestamp.")
        Instant statusExpiration;

        @Schema(title = "The user's avatar hash.")
        String avatarHash;

        @Schema(title = "The user's email address.")
        String email;

        @Schema(title = "The URL to the user's 24x24 profile image.")
        String image24;

        @Schema(title = "The URL to the user's 32x32 profile image.")
        String image32;

        @Schema(title = "The URL to the user's 48x48 profile image.")
        String image48;

        @Schema(title = "The URL to the user's 72x72 profile image.")
        String image72;

        @Schema(title = "The URL to the user's 192x192 profile image.")
        String image192;

        @Schema(title = "The URL to the user's 512x512 profile image.")
        String image512;

        @Schema(title = "The team ID.")
        String team;

        public static UserProfile of(com.slack.api.model.User.Profile profile) {
            if (profile == null) {
                return null;
            }

            return UserProfile.builder()
                .title(profile.getTitle())
                .phone(profile.getPhone())
                .skype(profile.getSkype())
                .realName(profile.getRealName())
                .realNameNormalized(profile.getRealNameNormalized())
                .displayName(profile.getDisplayName())
                .displayNameNormalized(profile.getDisplayNameNormalized())
                .statusText(profile.getStatusText())
                .statusEmoji(profile.getStatusEmoji())
                .statusExpiration(MessageService.fromSlackTimestamp(profile.getStatusExpiration()))
                .avatarHash(profile.getAvatarHash())
                .email(profile.getEmail())
                .image24(profile.getImage24())
                .image32(profile.getImage32())
                .image48(profile.getImage48())
                .image72(profile.getImage72())
                .image192(profile.getImage192())
                .image512(profile.getImage512())
                .team(profile.getTeam())
                .build();
        }
    }
}
