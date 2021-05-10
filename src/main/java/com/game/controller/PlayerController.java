package com.game.controller;

import com.game.entity.Player;
import com.game.repository.PlayerRepository;
import com.game.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rest/players")
public class PlayerController {

    private final PlayerRepository playerRepository;
    private final PlayerService playerService;

    @Autowired
    public PlayerController(PlayerRepository playerRepository, PlayerService playerService) {
        this.playerRepository = playerRepository;
        this.playerService = playerService;
    }

    @GetMapping
    public List<Player> getAllPlayers(@RequestParam(required = false) Map<String, String> params) {
        return playerService.filterPlayers(params, true);
    }

    @GetMapping("/count")
    public Integer getPlayersCount(@RequestParam(required = false) Map<String, String> params) {
        return playerService.filterPlayers(params, false).size();
    }

    @GetMapping("/{id}")
    public Player getPlayer(@PathVariable("id") Long id) {
        playerService.validateId(id);

        return playerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/")
    public ResponseEntity<Player> createPlayer(@RequestBody Player player) {
        playerService.processEntity(player);
        playerRepository.save(player);
        return ResponseEntity.status(HttpStatus.OK).body(player);
    }

    @PostMapping("/{id}")
    public ResponseEntity<Player> updatePlayer(@PathVariable("id") Long id, @RequestBody Player player) {
        Player existingPlayer = getPlayer(id);
        playerService.copyValues(existingPlayer, player);
        playerService.processEntity(existingPlayer);
        playerRepository.save(existingPlayer);

        return ResponseEntity.status(HttpStatus.OK).body(existingPlayer);
    }

    @DeleteMapping("/{id}")
    public void deletePlayer(@PathVariable("id") Long id) {
        playerService.validateId(id);

        getPlayer(id);
        playerRepository.deleteById(id);
    }
}
