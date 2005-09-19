#!/bin/sh

# -------------------------------------------------------------
# This process creates the application configuration properties
# ------------------------------------------------------------

envProperties=/t3/build/cruisecontrol/checkout/t3/library/config
templateProperties=/t3/build/cruisecontrol/checkout/t3/library/src/application/t3PortalApp/portal_properties/internal

envs="wlpdev1 wlptest1 wlpqa1 wlppp1"

for i in $envs
do
  cp $envProperties/env.properties.iportal.${i} ${i}.properties  
done

mv wlpdev1.properties dev.properties
mv wlptest1.properties test.properties
mv wlpqa1.properties qa.properties
mv wlppp1.properties prod.properties

mkdir env

mv *.properties env

mkdir app

cp $templateProperties/portal.properties app
cp $templateProperties/config.properties app
cp $templateProperties/brioreport.properties app
