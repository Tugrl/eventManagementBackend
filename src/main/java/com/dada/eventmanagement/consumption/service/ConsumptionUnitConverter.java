package com.dada.eventmanagement.consumption.service;

import com.dada.eventmanagement.common.enums.InventoryUnit;
import com.dada.eventmanagement.common.exception.BadRequestException;
import com.dada.eventmanagement.inventory.entity.InventoryItem;
import java.math.BigDecimal;
import java.math.RoundingMode;

final class ConsumptionUnitConverter {
    private static final BigDecimal THOUSAND = BigDecimal.valueOf(1000);

    private ConsumptionUnitConverter() {
    }

    static BigDecimal convertToStockUnit(InventoryItem item, InventoryUnit inputUnit, BigDecimal amountPerPerson) {
        if (item.getUnit() == inputUnit) {
            return amountPerPerson;
        }

        return switch (item.getUnit()) {
            case BOTTLE -> convertBottleItem(item, inputUnit, amountPerPerson);
            case MILLILITER -> convertMilliliterItem(inputUnit, amountPerPerson);
            case LITER -> convertLiterItem(inputUnit, amountPerPerson);
            case GRAM -> convertGramItem(inputUnit, amountPerPerson);
            case KILOGRAM -> convertKilogramItem(inputUnit, amountPerPerson);
            case PIECE -> throw new BadRequestException("Adet bazli urunlerde farkli tuketim birimi kullanilamaz");
        };
    }

    private static BigDecimal convertBottleItem(InventoryItem item, InventoryUnit inputUnit, BigDecimal amountPerPerson) {
        BigDecimal bottleVolumeMl = item.getBottleVolumeMl();
        if (bottleVolumeMl == null || bottleVolumeMl.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Siseli urunlerde sise hacmi tanimli olmali");
        }

        return switch (inputUnit) {
            case BOTTLE -> amountPerPerson;
            case MILLILITER -> amountPerPerson.divide(bottleVolumeMl, 6, RoundingMode.HALF_UP);
            case LITER -> amountPerPerson.multiply(THOUSAND).divide(bottleVolumeMl, 6, RoundingMode.HALF_UP);
            default -> throw new BadRequestException("Bu urun icin secilen tuketim birimi desteklenmiyor");
        };
    }

    private static BigDecimal convertMilliliterItem(InventoryUnit inputUnit, BigDecimal amountPerPerson) {
        return switch (inputUnit) {
            case MILLILITER -> amountPerPerson;
            case LITER -> amountPerPerson.multiply(THOUSAND);
            default -> throw new BadRequestException("Ml bazli urunlerde sadece ml veya litre kullanilabilir");
        };
    }

    private static BigDecimal convertLiterItem(InventoryUnit inputUnit, BigDecimal amountPerPerson) {
        return switch (inputUnit) {
            case LITER -> amountPerPerson;
            case MILLILITER -> amountPerPerson.divide(THOUSAND, 6, RoundingMode.HALF_UP);
            default -> throw new BadRequestException("Litre bazli urunlerde sadece litre veya ml kullanilabilir");
        };
    }

    private static BigDecimal convertGramItem(InventoryUnit inputUnit, BigDecimal amountPerPerson) {
        return switch (inputUnit) {
            case GRAM -> amountPerPerson;
            case KILOGRAM -> amountPerPerson.multiply(THOUSAND);
            default -> throw new BadRequestException("Gram bazli urunlerde sadece gram veya kilogram kullanilabilir");
        };
    }

    private static BigDecimal convertKilogramItem(InventoryUnit inputUnit, BigDecimal amountPerPerson) {
        return switch (inputUnit) {
            case KILOGRAM -> amountPerPerson;
            case GRAM -> amountPerPerson.divide(THOUSAND, 6, RoundingMode.HALF_UP);
            default -> throw new BadRequestException("Kilogram bazli urunlerde sadece kilogram veya gram kullanilabilir");
        };
    }
}
