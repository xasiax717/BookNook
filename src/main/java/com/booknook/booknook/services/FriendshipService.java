package com.booknook.booknook.services;

import com.booknook.booknook.entities.Friendship;
import com.booknook.booknook.entities.User;
import com.booknook.booknook.repositories.FriendshipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;

    public List<Friendship> getAcceptedFriends(User user) {
        return friendshipRepository.findAllAcceptedFriends(user);
    }

    public void sendFriendRequest(User requester, User addressee) {
        // Sprawdzamy, czy relacja już nie istnieje
        if (friendshipRepository.findRelation(requester, addressee).isPresent()) {
            return;
        }
        Friendship friendship = new Friendship();
        friendship.setRequester(requester);
        friendship.setAddressee(addressee);
        friendship.setStatus(Friendship.FriendshipStatus.PENDING);
        friendshipRepository.save(friendship);
    }

    @Transactional
    public void deleteFriendship(Long id) {
        friendshipRepository.deleteById(id);
    }

    public List<Friendship> getPendingRequests(User user) {
        List<Friendship> requests = friendshipRepository.findAllPendingRequests(user);
        System.out.println("DEBUG: Liczba znalezionych zaproszeń dla " + user.getUsername() + ": " + requests.size());
        return requests;
    }

    @Transactional
    public void acceptFriendship(Long id) {
        Friendship f = friendshipRepository.findById(id).orElseThrow();
        f.setStatus(Friendship.FriendshipStatus.ACCEPTED);
        friendshipRepository.save(f);
    }
}