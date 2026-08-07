package com.example.demo.price.infrastructure;

import com.example.demo.price.domain.ChannelCode;
import com.example.demo.price.domain.ItemEntity;
import com.example.demo.price.domain.OnlineChannelEntity;
import com.example.demo.price.domain.PriceUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PriceCollectionSeedInitializer implements CommandLineRunner {

    private static final List<ItemSeed> ITEM_SEEDS = List.of(
            new ItemSeed(152, "감자", PriceUnit.KG),
            new ItemSeed(258, "마늘", PriceUnit.KG),
            new ItemSeed(245, "양파", PriceUnit.KG),
            new ItemSeed(151, "고구마", PriceUnit.KG),
            new ItemSeed(232, "당근", PriceUnit.KG),
            new ItemSeed(225, "토마토", PriceUnit.KG),
            new ItemSeed(255, "피망", PriceUnit.KG),
            new ItemSeed(211, "배추", PriceUnit.COUNT),
            new ItemSeed(231, "무", PriceUnit.COUNT),
            new ItemSeed(224, "애호박", PriceUnit.COUNT),
            new ItemSeed(224, "쥬키니", PriceUnit.COUNT),
            new ItemSeed(223, "오이", PriceUnit.COUNT),
            new ItemSeed(422, "방울토마토", PriceUnit.COUNT),
            new ItemSeed(422, "대추방울토마토", PriceUnit.COUNT),
            new ItemSeed(246, "대파", PriceUnit.KG),
            new ItemSeed(246, "쪽파", PriceUnit.KG),
            new ItemSeed(242, "풋고추", PriceUnit.KG),
            new ItemSeed(242, "꽈리고추", PriceUnit.KG),
            new ItemSeed(242, "청양고추", PriceUnit.KG),
            new ItemSeed(242, "오이맛고추", PriceUnit.KG),
            new ItemSeed(315, "느타리버섯", PriceUnit.G),
            new ItemSeed(316, "팽이버섯", PriceUnit.G),
            new ItemSeed(317, "새송이버섯", PriceUnit.G),
            new ItemSeed(312, "참깨", PriceUnit.G),
            new ItemSeed(314, "땅콩", PriceUnit.G),
            new ItemSeed(212, "양배추", PriceUnit.COUNT),
            new ItemSeed(213, "시금치", PriceUnit.G),
            new ItemSeed(215, "얼갈이배추", PriceUnit.KG),
            new ItemSeed(233, "열무", PriceUnit.KG),
            new ItemSeed(241, "건고추", PriceUnit.G),
            new ItemSeed(243, "붉은고추", PriceUnit.G),
            new ItemSeed(247, "생강", PriceUnit.KG),
            new ItemSeed(248, "고춧가루-국산", PriceUnit.KG),
            new ItemSeed(248, "고춧가루-중국산", PriceUnit.KG),
            new ItemSeed(252, "미나리", PriceUnit.G),
            new ItemSeed(253, "깻잎", PriceUnit.G),
            new ItemSeed(256, "파프리카", PriceUnit.G),
            new ItemSeed(257, "멜론", PriceUnit.COUNT),
            new ItemSeed(280, "브로콜리", PriceUnit.COUNT),
            new ItemSeed(279, "알배기배추", PriceUnit.COUNT),
            new ItemSeed(221, "수박", PriceUnit.COUNT),
            new ItemSeed(222, "참외", PriceUnit.COUNT),
            new ItemSeed(226, "딸기", PriceUnit.G),
            new ItemSeed(214, "적상추", PriceUnit.G),
            new ItemSeed(214, "청상추", PriceUnit.G),
            new ItemSeed(216, "갓", PriceUnit.KG));

    private static final List<ChannelSeed> CHANNEL_SEEDS = List.of(
            new ChannelSeed(1, ChannelCode.OASIS, "오아시스", true),
            new ChannelSeed(2, ChannelCode.KURLY, "컬리", false),
            new ChannelSeed(3, ChannelCode.ELEVEN_ST, "11번가", false),
            new ChannelSeed(4, ChannelCode.GS_SHOP, "GS SHOP", false));

    private final ItemJpaRepository itemRepository;
    private final OnlineChannelJpaRepository channelRepository;

    @Override
    public void run(final String... args) {
        seedItems();
        seedChannels();
    }

    private void seedItems() {
        for (ItemSeed seed : ITEM_SEEDS) {
            if (itemRepository.findByItemCodeAndName(seed.itemCode(), seed.name()).isEmpty()) {
                itemRepository.save(new ItemEntity(
                        seed.itemCode(), seed.name(), seed.targetUnit(), true));
            }
        }
    }

    private void seedChannels() {
        for (ChannelSeed seed : CHANNEL_SEEDS) {
            if (channelRepository.findByCode(seed.code()) == null) {
                channelRepository.save(new OnlineChannelEntity(
                        seed.id(), seed.code(), seed.name(), seed.active()));
            }
        }
    }

    private record ChannelSeed(Integer id, ChannelCode code, String name, boolean active) {}

    private record ItemSeed(Integer itemCode, String name, PriceUnit targetUnit) {}
}
