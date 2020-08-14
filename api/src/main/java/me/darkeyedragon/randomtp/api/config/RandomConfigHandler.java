package me.darkeyedragon.randomtp.api.config;

import me.darkeyedragon.randomtp.api.config.section.*;

interface RandomConfigHandler {
    SectionDebug getSectionDebug();
    SectionEconomy getSectionEconomy();
    SectionMessage getSectionMessage();
    SectionPlugin getSectionPlugin();
    SectionQueue getSectionQueue();
    SectionTeleport getSectionTeleport();
    SectionWorld getSectionWorld();
}
