Maven 1.x Integration

This component will internally spawn a complete Maven 1.x instance
that will run a project from within m2, using a specified version of
Maven 1.x.

This will allow a mix of m2 and m1 projects in a single organization
with only one installation, aiding the transition.

Work in progress, Brett Porter, 18/8/04


Currently it does require an m1 installation to obtain the plugins installed. It could potentially use
the plugin descriptor file for this.
