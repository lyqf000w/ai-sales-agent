package cn.ai.sales.module.agent.service.gewe;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record GeweContactListResult(List<String> friends,
                                    List<String> chatrooms,
                                    List<String> officialAccounts,
                                    Map<String, Object> rawResponse) {

    public List<String> syncableContactIds() {
        return Stream.concat(
                        friends == null ? Stream.empty() : friends.stream(),
                        chatrooms == null ? Stream.empty() : chatrooms.stream())
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .toList();
    }

    public GeweContactListResult merge(GeweContactListResult other) {
        if (other == null) {
            return this;
        }
        return new GeweContactListResult(mergeList(friends, other.friends()),
                mergeList(chatrooms, other.chatrooms()),
                mergeList(officialAccounts, other.officialAccounts()),
                rawResponse != null ? rawResponse : other.rawResponse());
    }

    private static List<String> mergeList(List<String> first, List<String> second) {
        return Stream.concat(first == null ? Stream.empty() : first.stream(),
                        second == null ? Stream.empty() : second.stream())
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .toList();
    }

}
