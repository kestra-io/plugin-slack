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
    @Schema(title = "User ID")
    String id;

    @Schema(title = "Team ID")
    String teamId;

    @Schema(title = "Username")
    String name;

    @Schema(title = "Is deleted")
    Boolean deleted;

    @Schema(title = "User color")
    String color;

    @Schema(title = "Real name")
    String realName;

    @Schema(title = "Timezone")
    String tz;

    @Schema(title = "Timezone label")
    String tzLabel;

    @Schema(title = "Timezone offset")
    Integer tzOffset;

    @Schema(title = "User profile")
    UserProfile profile;

    @Schema(title = "Is admin")
    Boolean isAdmin;

    @Schema(title = "Is owner")
    Boolean isOwner;

    @Schema(title = "Is primary owner")
    Boolean isPrimaryOwner;

    @Schema(title = "Is restricted")
    Boolean isRestricted;

    @Schema(title = "Is ultra restricted")
    Boolean isUltraRestricted;

    @Schema(title = "Is bot")
    Boolean isBot;

    @Schema(title = "Is app user")
    Boolean isAppUser;

    @Schema(title = "Last updated")
    Instant updated;

    @Schema(title = "Has 2FA")
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
        @Schema(title = "Title")
        String title;

        @Schema(title = "Phone number")
        String phone;

        @Schema(title = "Skype name")
        String skype;

        @Schema(title = "Real name")
        String realName;

        @Schema(title = "Real name (normalized)")
        String realNameNormalized;

        @Schema(title = "Display name")
        String displayName;

        @Schema(title = "Display name (normalized)")
        String displayNameNormalized;

        @Schema(title = "Status text")
        String statusText;

        @Schema(title = "Status emoji")
        String statusEmoji;

        @Schema(title = "Status expiration")
        Instant statusExpiration;

        @Schema(title = "Avatar hash")
        String avatarHash;

        @Schema(title = "Email")
        String email;

        @Schema(title = "Image 24x24 URL")
        String image24;

        @Schema(title = "Image 32x32 URL")
        String image32;

        @Schema(title = "Image 48x48 URL")
        String image48;

        @Schema(title = "Image 72x72 URL")
        String image72;

        @Schema(title = "Image 192x192 URL")
        String image192;

        @Schema(title = "Image 512x512 URL")
        String image512;

        @Schema(title = "Team ID")
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
